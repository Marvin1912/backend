package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.CreateMealTemplateRequest;
import com.marvin.nutrition.dto.MealTemplateDTO;
import com.marvin.nutrition.dto.SaveEstimateAsTemplateRequest;
import com.marvin.nutrition.dto.UpdateMealTemplateRequest;
import com.marvin.nutrition.service.MealTemplateService;
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

/**
 * REST controller for managing reusable meal templates.
 * Provides endpoints to create, read, update and delete meal templates and their item composition.
 */
@RestController
@RequestMapping("/nutrition/meal-templates")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class MealTemplateController {

    private static final String TEMPLATES_LOCATION_PREFIX = "/nutrition/meal-templates/";

    private final MealTemplateService mealTemplateService;

    /**
     * Creates a new MealTemplateController with the required service.
     *
     * @param mealTemplateService the service handling meal template operations
     */
    public MealTemplateController(MealTemplateService mealTemplateService) {
        this.mealTemplateService = mealTemplateService;
    }

    /**
     * Lists all meal templates ordered alphabetically by name, with each item's macros live-computed
     * from the current food catalog.
     *
     * @return a Mono emitting the list of meal template DTOs
     */
    @GetMapping
    @Operation(
            summary = "List meal templates",
            description = "Returns all meal templates ordered by name, with each item's macros live-computed "
                    + "from the current food catalog.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Meal template list returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = MealTemplateDTO.class)))
                )
            }
    )
    public Mono<List<MealTemplateDTO>> listTemplates() {
        return mealTemplateService.findAll();
    }

    /**
     * Returns the meal template with the given id, or 404 if not found.
     *
     * @param id the UUID of the meal template
     * @return a Mono with 200 and the meal template DTO, or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a meal template by id",
            description = "Returns a single meal template by its UUID, with each item's macros live-computed "
                    + "from the current food catalog.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Meal template returned",
                        content = @Content(schema = @Schema(implementation = MealTemplateDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Meal template not found")
            }
    )
    public Mono<ResponseEntity<MealTemplateDTO>> getTemplateById(
            @PathVariable @Parameter(description = "UUID of the meal template") UUID id) {
        return mealTemplateService.findById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Creates a new meal template and returns 201 Created with a Location header.
     *
     * @param req the create request body
     * @return a Mono with 201 Created, Location header, and the created meal template DTO
     */
    @PostMapping
    @Operation(
            summary = "Create a meal template",
            description = "Creates a new meal template with the given name and items. An empty items list is "
                    + "allowed. Every referenced foodId must exist in the food catalog.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Meal template created; Location header contains the URI",
                        content = @Content(schema = @Schema(implementation = MealTemplateDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "A referenced food was not found")
            }
    )
    public Mono<ResponseEntity<MealTemplateDTO>> createTemplate(@Valid @RequestBody CreateMealTemplateRequest req) {
        return mealTemplateService.create(req)
                .map(created -> {
                    final URI location = URI.create(TEMPLATES_LOCATION_PREFIX + created.id());
                    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
                });
    }

    /**
     * Atomically creates a synthetic food entry and a meal template from a canteen AI estimate,
     * returning 201 Created with a Location header.
     *
     * @param req the estimate request body
     * @return a Mono with 201 Created, Location header, and the created meal template DTO
     */
    @PostMapping("/from-estimate")
    @Operation(
            summary = "Create a meal template from a canteen AI estimate",
            description = "Atomically creates a synthetic food entry (source=ESTIMATE) and a meal template "
                    + "containing that food at 100 g. The macro values are stored as-is on the food entry.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Meal template created; Location header contains the URI",
                        content = @Content(schema = @Schema(implementation = MealTemplateDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed")
            }
    )
    public Mono<ResponseEntity<MealTemplateDTO>> createFromEstimate(@Valid @RequestBody SaveEstimateAsTemplateRequest req) {
        return mealTemplateService.createFromEstimate(req)
                .map(created -> {
                    final URI location = URI.create(TEMPLATES_LOCATION_PREFIX + created.id());
                    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
                });
    }

    /**
     * Replaces the name and entire item composition of an existing meal template, or returns 404 if not found.
     *
     * @param id  the UUID of the meal template to update
     * @param req the update request body
     * @return a Mono with 200 and the updated meal template DTO, or 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a meal template",
            description = "Fully replaces an existing meal template: renames it and replaces its entire item "
                    + "composition in one call. Items not present in the request are removed, and all items in "
                    + "the request are (re-)created. An empty items list removes all items. Every referenced "
                    + "foodId must exist in the food catalog.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Meal template updated",
                        content = @Content(schema = @Schema(implementation = MealTemplateDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Meal template not found, or a referenced food was not found")
            }
    )
    public Mono<ResponseEntity<MealTemplateDTO>> updateTemplate(
            @PathVariable @Parameter(description = "UUID of the meal template to update") UUID id,
            @Valid @RequestBody UpdateMealTemplateRequest req) {
        return mealTemplateService.update(id, req)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes the meal template with the given id, or returns 404 if not found.
     *
     * @param id the UUID of the meal template to delete
     * @return a Mono with 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a meal template",
            description = "Permanently removes the meal template and its items.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Meal template deleted"),
                @ApiResponse(responseCode = "404", description = "Meal template not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteTemplate(
            @PathVariable @Parameter(description = "UUID of the meal template to delete") UUID id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        return mealTemplateService.delete(id)
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound);
    }
}
