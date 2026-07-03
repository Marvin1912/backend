package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.CreateMealPlanChangelogEntryRequest;
import com.marvin.nutrition.dto.MealPlanChangelogEntryDTO;
import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.MealPlanStatDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingCategoryRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingItemRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
import com.marvin.nutrition.dto.UpdateMealPlanStatRequest;
import com.marvin.nutrition.service.MealPlanService;
import com.marvin.nutrition.service.MealPlanWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * The document is assembled from normalized database tables; its content (section text, row
 * details/macros, headline stats, shopping list, changelog, header text) can be corrected through
 * this API instead of hand-written SQL or new Flyway migrations.
 */
@RestController
@RequestMapping("/nutrition/meal-plan")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class MealPlanController {

    private static final String CHANGELOG_LOCATION_PREFIX = "/nutrition/meal-plan/changelog/";

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
            description = "Returns the weekly meal-plan reference document, including the shopping list.",
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
            description = "Applies partial updates to the meal plan's header (eyebrow, title, description, "
                    + "shopping-list title/note/callout, footer note).",
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
            description = "Applies partial updates to an existing meal-plan section's title, note, callout and/or "
                    + "totals row (label, kcal, protein).",
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
     * Updates a meal-plan row, or returns 404 if not found.
     *
     * @param id  the UUID of the row to update
     * @param req the update request body
     * @return a Mono with 200 and the updated row DTO, or 404 if not found
     */
    @PutMapping("/rows/{id}")
    @Operation(
            summary = "Update a meal-plan row",
            description = "Applies partial updates to an existing meal-plan row's meal, details, quantity, "
                    + "kcal and/or protein.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Row updated",
                        content = @Content(schema = @Schema(implementation = MealPlanRowDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Row not found")
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
     * Updates a meal-plan headline statistic, or returns 404 if not found.
     *
     * @param id  the UUID of the stat to update
     * @param req the update request body
     * @return a Mono with 200 and the updated stat DTO, or 404 if not found
     */
    @PutMapping("/stats/{id}")
    @Operation(
            summary = "Update a meal-plan headline statistic",
            description = "Applies partial updates to an existing headline statistic's label and/or value.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Stat updated",
                        content = @Content(schema = @Schema(implementation = MealPlanStatDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Stat not found")
            }
    )
    public Mono<ResponseEntity<MealPlanStatDTO>> updateStat(
            @PathVariable @Parameter(description = "UUID of the stat to update") UUID id,
            @Valid @RequestBody UpdateMealPlanStatRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.updateStat(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Updates a meal-plan shopping-list category, or returns 404 if not found.
     *
     * @param id  the UUID of the category to update
     * @param req the update request body
     * @return a Mono with 200 and the updated category DTO, or 404 if not found
     */
    @PutMapping("/shopping-categories/{id}")
    @Operation(
            summary = "Update a meal-plan shopping-list category",
            description = "Applies partial updates to an existing shopping-list category's title.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Shopping category updated",
                        content = @Content(schema = @Schema(implementation = MealPlanShoppingCategoryDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Shopping category not found")
            }
    )
    public Mono<ResponseEntity<MealPlanShoppingCategoryDTO>> updateShoppingCategory(
            @PathVariable @Parameter(description = "UUID of the shopping category to update") UUID id,
            @Valid @RequestBody UpdateMealPlanShoppingCategoryRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.updateShoppingCategory(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Updates a meal-plan shopping-list item, or returns 404 if not found.
     *
     * @param id  the UUID of the item to update
     * @param req the update request body
     * @return a Mono with 200 and the updated item DTO, or 404 if not found
     */
    @PutMapping("/shopping-items/{id}")
    @Operation(
            summary = "Update a meal-plan shopping-list item",
            description = "Applies partial updates to an existing shopping-list item's name, brand, badge, "
                    + "badge text and/or quantity.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Shopping item updated",
                        content = @Content(schema = @Schema(implementation = MealPlanShoppingItemDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Shopping item not found")
            }
    )
    public Mono<ResponseEntity<MealPlanShoppingItemDTO>> updateShoppingItem(
            @PathVariable @Parameter(description = "UUID of the shopping item to update") UUID id,
            @Valid @RequestBody UpdateMealPlanShoppingItemRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.updateShoppingItem(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
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
     * Appends a new entry to the meal plan's changelog and returns 201 Created with a Location header.
     *
     * @param req the create request body
     * @return a Mono with 201 Created, Location header, and the created changelog entry DTO
     */
    @PostMapping("/changelog")
    @Operation(
            summary = "Append a meal-plan changelog entry",
            description = "Appends a new entry to the meal plan's changelog. The changelog is an append-only "
                    + "historical log; there is no update or delete operation for existing entries.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Changelog entry created; Location header contains the URI",
                        content = @Content(schema = @Schema(implementation = MealPlanChangelogEntryDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed or missing required fields")
            }
    )
    public Mono<ResponseEntity<MealPlanChangelogEntryDTO>> addChangelogEntry(
            @Valid @RequestBody CreateMealPlanChangelogEntryRequest req) {
        return Mono.fromCallable(() -> mealPlanWriteService.addChangelogEntry(req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(created -> {
                    final URI location = URI.create(CHANGELOG_LOCATION_PREFIX + created.id());
                    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
                });
    }
}
