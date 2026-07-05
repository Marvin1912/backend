package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.CreateMealPlanRowRequest;
import com.marvin.nutrition.dto.CreateMealPlanRowsRequest;
import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
import com.marvin.nutrition.service.MealPlanService;
import com.marvin.nutrition.service.MealPlanWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST controller serving the weekly meal-plan reference document and its content-write endpoints.
 * The document is assembled from normalized database tables; rows are food-backed and their macros
 * are derived server-side. Content (section text, header text, footer sources, and food-backed rows)
 * can be corrected through this API instead of hand-written SQL or new Flyway migrations.
 */
@RestController
@RequestMapping("/nutrition/meal-plan")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class MealPlanController {

    private static final String ROWS_LOCATION_PREFIX = "/nutrition/meal-plan/rows/";

    private final MealPlanService mealPlanService;
    private final MealPlanWriteService mealPlanWriteService;

    /**
     * Creates a new MealPlanController.
     *
     * @param mealPlanService      the service serving the assembled meal-plan document
     * @param mealPlanWriteService the service owning the meal-plan's transactional write operations
     */
    public MealPlanController(MealPlanService mealPlanService, MealPlanWriteService mealPlanWriteService) {
        this.mealPlanService = mealPlanService;
        this.mealPlanWriteService = mealPlanWriteService;
    }

    /**
     * Returns the weekly meal-plan reference document.
     *
     * @return a Mono emitting the meal-plan DTO
     */
    @GetMapping
    @Operation(
            summary = "Get the weekly meal plan",
            description = "Returns the weekly meal-plan reference document.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Meal plan returned",
                        content = @Content(schema = @Schema(implementation = MealPlanDTO.class))
                )
            }
    )
    public Mono<MealPlanDTO> getMealPlan() {
        return mealPlanService.getMealPlan();
    }

    /**
     * Updates the meal plan's header content, or returns 404 if the singleton row is missing.
     *
     * @param req the update request body
     * @return a Mono with 200 and the updated header DTO, or 404 if not found
     */
    @PutMapping
    @Operation(
            summary = "Update the meal plan's header content",
            description = "Applies partial updates to the meal plan's header (eyebrow, title, description, footer note).",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Meal plan header updated",
                        content = @Content(schema = @Schema(implementation = MealPlanHeaderDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Meal plan not found")
            }
    )
    public Mono<ResponseEntity<MealPlanHeaderDTO>> updateMealPlan(@Valid @RequestBody UpdateMealPlanRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.updateMealPlan(req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Updates a meal-plan section, or returns 404 if not found.
     *
     * @param id  the UUID of the section to update
     * @param req the update request body
     * @return a Mono with 200 and the updated section DTO, or 404 if not found
     */
    @PutMapping("/sections/{id}")
    @Operation(
            summary = "Update a meal-plan section",
            description = "Applies partial updates to an existing meal-plan section's title, note and/or callout.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Section updated",
                        content = @Content(schema = @Schema(implementation = MealPlanSectionDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Section not found")
            }
    )
    public Mono<ResponseEntity<MealPlanSectionDTO>> updateSection(
            @PathVariable @Parameter(description = "UUID of the section to update") UUID id,
            @Valid @RequestBody UpdateMealPlanSectionRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.updateSection(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Creates a new food-backed row within the given section and returns 201 Created with a Location header.
     *
     * @param sectionId the UUID of the section to add the row to
     * @param req       the create request body
     * @return a Mono with 201 Created, Location header, and the created row DTO
     */
    @PostMapping("/sections/{sectionId}/rows")
    @Operation(
            summary = "Create a meal-plan row",
            description = "Adds a food-backed row to the given section. Macros are derived server-side "
                    + "from the referenced food's per-100g values and the supplied quantity.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Row created; Location header contains the URI",
                        content = @Content(schema = @Schema(implementation = MealPlanRowDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Section or referenced food not found")
            }
    )
    public Mono<ResponseEntity<MealPlanRowDTO>> addRow(
            @PathVariable @Parameter(description = "UUID of the section to add the row to") UUID sectionId,
            @Valid @RequestBody CreateMealPlanRowRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.addRow(sectionId, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(created -> {
                    final URI location = URI.create(ROWS_LOCATION_PREFIX + created.id());
                    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
                })
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Creates multiple food-backed rows within the given section in a single database transaction and
     * returns 201 Created with the created rows as a JSON array (no Location header).
     *
     * @param sectionId the UUID of the section to add the rows to
     * @param req       the batch create request body, containing a non-empty list of rows
     * @return a Mono with 201 Created and the created row DTOs, or 404 if the section or any referenced food is not found
     */
    @PostMapping("/sections/{sectionId}/rows/batch")
    @Operation(
            summary = "Create multiple meal-plan rows in one transaction",
            description = "Adds multiple food-backed rows to the given section in a single database "
                    + "transaction. If any referenced food is unknown, the whole batch is rolled back. "
                    + "The response body is a JSON array and no Location header is set.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Rows created",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = MealPlanRowDTO.class)))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed: empty list or invalid row"),
                @ApiResponse(responseCode = "404", description = "Section or a referenced food not found; nothing was persisted")
            }
    )
    public Mono<ResponseEntity<List<MealPlanRowDTO>>> addRows(
            @PathVariable @Parameter(description = "UUID of the section to add the rows to") UUID sectionId,
            @Valid @RequestBody CreateMealPlanRowsRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.addRows(sectionId, req.rows()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Updates a meal-plan row, or returns 404 if not found.
     *
     * @param id  the UUID of the row to update
     * @param req the update request body
     * @return a Mono with 200 and the updated row DTO, or 404 if not found
     */
    @PutMapping("/rows/{id}")
    @Operation(
            summary = "Update a meal-plan row",
            description = "Applies updates to an existing meal-plan row's meal type, referenced food "
                    + "and/or quantity. Macros are re-snapshotted from the referenced food on every update.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Row updated",
                        content = @Content(schema = @Schema(implementation = MealPlanRowDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Row or referenced food not found")
            }
    )
    public Mono<ResponseEntity<MealPlanRowDTO>> updateRow(
            @PathVariable @Parameter(description = "UUID of the row to update") UUID id,
            @Valid @RequestBody UpdateMealPlanRowRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.updateRow(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes a meal-plan row, or returns 404 if not found.
     *
     * @param id the UUID of the row to delete
     * @return a Mono with 204 on success, or 404 if not found
     */
    @DeleteMapping("/rows/{id}")
    @Operation(
            summary = "Delete a meal-plan row",
            description = "Permanently removes the meal-plan row.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Row deleted"),
                @ApiResponse(responseCode = "404", description = "Row not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteRow(
            @PathVariable @Parameter(description = "UUID of the row to delete") UUID id) {
        return Mono.fromRunnable(() -> mealPlanWriteService.deleteRow(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Updates a meal-plan footer source, or returns 404 if not found.
     *
     * @param id  the UUID of the source to update
     * @param req the update request body
     * @return a Mono with 200 and the updated source DTO, or 404 if not found
     */
    @PutMapping("/sources/{id}")
    @Operation(
            summary = "Update a meal-plan footer source",
            description = "Applies partial updates to an existing footer source's label and/or URL.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Source updated",
                        content = @Content(schema = @Schema(implementation = MealPlanSourceDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Source not found")
            }
    )
    public Mono<ResponseEntity<MealPlanSourceDTO>> updateSource(
            @PathVariable @Parameter(description = "UUID of the source to update") UUID id,
            @Valid @RequestBody UpdateMealPlanSourceRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.updateSource(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes a meal-plan footer source, or returns 404 if not found.
     *
     * @param id the UUID of the source to delete
     * @return a Mono with 204 on success, or 404 if not found
     */
    @DeleteMapping("/sources/{id}")
    @Operation(
            summary = "Delete a meal-plan footer source",
            description = "Deletes an existing footer source.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Source deleted"),
                @ApiResponse(responseCode = "404", description = "Source not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteSource(
            @PathVariable @Parameter(description = "UUID of the source to delete") UUID id) {
        return Mono.fromRunnable(() -> mealPlanWriteService.deleteSource(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }
}
