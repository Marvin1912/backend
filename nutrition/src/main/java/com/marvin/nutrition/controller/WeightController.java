package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.CreateWeightEntryRequest;
import com.marvin.nutrition.dto.WeightEntryDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioSummaryResponse;
import com.marvin.nutrition.service.WeightNutrientRatioService;
import com.marvin.nutrition.service.WeightNutrientRatioSummaryService;
import com.marvin.nutrition.service.WeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for body-weight log management.
 * Supports listing, creating, updating and deleting weight entries.
 */
@RestController
@RequestMapping("/nutrition/weight")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class WeightController {

    private static final String WEIGHT_LOCATION_PREFIX = "/nutrition/weight/";

    private final WeightService weightService;
    private final WeightNutrientRatioService weightNutrientRatioService;
    private final WeightNutrientRatioSummaryService weightNutrientRatioSummaryService;

    /**
     * Creates a new WeightController.
     *
     * @param weightService                      the service handling weight entry operations
     * @param weightNutrientRatioService         the service computing weight/nutrient-intake ratios
     * @param weightNutrientRatioSummaryService  the service computing weight/nutrient-ratio period summaries
     */
    public WeightController(
            WeightService weightService,
            WeightNutrientRatioService weightNutrientRatioService,
            WeightNutrientRatioSummaryService weightNutrientRatioSummaryService) {
        this.weightService = weightService;
        this.weightNutrientRatioService = weightNutrientRatioService;
        this.weightNutrientRatioSummaryService = weightNutrientRatioSummaryService;
    }

    /**
     * Returns all weight entries ordered by date descending.
     *
     * @return a Flux emitting all weight entry DTOs
     */
    @GetMapping
    @Operation(
            summary = "List weight entries",
            description = "Returns all body-weight measurements ordered by date descending.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Weight entries returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = WeightEntryDTO.class)))
                )
            }
    )
    public Flux<WeightEntryDTO> listWeightEntries() {
        return weightService.findAll();
    }

    /**
     * Creates a new weight entry and returns 201 Created with the new resource URI.
     *
     * @param request the entry date and weight in kg
     * @return a Mono with 201 Created and the created DTO
     */
    @PostMapping
    @Operation(
            summary = "Log a weight measurement",
            description = "Records a new body-weight measurement for the given date.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Weight entry created",
                        content = @Content(schema = @Schema(implementation = WeightEntryDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed")
            }
    )
    public Mono<ResponseEntity<WeightEntryDTO>> createWeightEntry(@Valid @RequestBody CreateWeightEntryRequest request) {
        return weightService.create(request)
                .map(dto -> {
                    final URI location = URI.create(WEIGHT_LOCATION_PREFIX + dto.id());
                    return ResponseEntity.created(location).body(dto);
                });
    }

    /**
     * Updates an existing weight entry, or returns 404 if it does not exist.
     *
     * @param id      the id of the entry to update
     * @param request the new entry date and weight
     * @return a Mono with 200 and the updated DTO, or 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a weight entry",
            description = "Replaces the date and weight of an existing entry.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Entry updated",
                        content = @Content(schema = @Schema(implementation = WeightEntryDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Entry not found"),
                @ApiResponse(responseCode = "400", description = "Validation failed")
            }
    )
    public Mono<ResponseEntity<WeightEntryDTO>> updateWeightEntry(
            @PathVariable @Parameter(description = "Id of the weight entry") Long id,
            @Valid @RequestBody CreateWeightEntryRequest request) {
        return weightService.update(id, request)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes a weight entry, or returns 404 if it does not exist.
     *
     * @param id the id of the entry to delete
     * @return a Mono with 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a weight entry",
            description = "Permanently removes the weight measurement with the given id.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Entry deleted"),
                @ApiResponse(responseCode = "404", description = "Entry not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteWeightEntry(
            @PathVariable @Parameter(description = "Id of the weight entry to delete") Long id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        return weightService.delete(id)
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound);
    }

    /**
     * Returns, for each date in the given range, the applicable historical body weight paired with
     * that day's total nutrient intake and the resulting per-kilogram ratios.
     *
     * @param from the first date to include (inclusive)
     * @param to   the last date to include (inclusive)
     * @return a Mono emitting one ratio entry per date in the range, ordered ascending by date
     */
    @GetMapping("/ratios")
    @Operation(
            summary = "Get weight/nutrient-intake ratios",
            description = "Returns, for each day in the given date range, the applicable historical body weight "
                    + "paired with that day's total nutrient intake (kcal, protein, carbs, fat) and the "
                    + "computed per-kilogram ratios.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Ratios returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = WeightNutrientRatioDTO.class)))
                )
            }
    )
    public Mono<List<WeightNutrientRatioDTO>> getRatios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "First date to include (inclusive)") LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Last date to include (inclusive)") LocalDate to) {
        return weightNutrientRatioService.getRatios(from, to);
    }

    /**
     * Returns the average macro-to-body-weight ratios for the last 30 days and for the entire
     * tracked period, each averaged over the days that have at least one logged meal entry.
     *
     * @return a Mono emitting both period summaries
     */
    @GetMapping("/ratios/summary")
    @Operation(
            summary = "Get weight/nutrient-intake ratio summary",
            description = "Returns the average macro-nutrient-to-body-weight ratios (protein, carbs, fat) for the "
                    + "last 30 days and for the entire tracked period, averaged over days with logged meals.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Summary returned",
                        content = @Content(schema = @Schema(implementation = WeightNutrientRatioSummaryResponse.class))
                )
            }
    )
    public Mono<WeightNutrientRatioSummaryResponse> getRatioSummary() {
        return weightNutrientRatioSummaryService.getSummary();
    }
}
