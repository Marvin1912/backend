package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.CreateMealEntryRequest;
import com.marvin.nutrition.dto.DaySummaryDTO;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.UpdateMealEntryRequest;
import com.marvin.nutrition.service.MealEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for meal logging and daily nutritional summary.
 * Provides endpoints to add, update and delete meal entries, and to retrieve the day summary.
 */
@RestController
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class MealEntryController {

    private static final String ENTRIES_LOCATION_PREFIX = "/nutrition/entries/";

    private final MealEntryService mealEntryService;

    /**
     * Creates a new MealEntryController with the required service.
     *
     * @param mealEntryService the service handling meal entry operations
     */
    public MealEntryController(MealEntryService mealEntryService) {
        this.mealEntryService = mealEntryService;
    }

    /**
     * Logs a new meal entry for the given date and returns 201 Created with a Location header.
     *
     * @param date the date to log the entry for
     * @param req  the create request body
     * @return a Mono with 201 Created, Location header, and the created meal entry DTO
     */
    @PostMapping("/nutrition/days/{date}/entries")
    @Operation(
            summary = "Log a meal entry",
            description = "Adds a meal entry for the given day. "
                    + "Supply foodId for food-backed entries (macros are snapshotted); "
                    + "omit foodId and supply description + macros for ad-hoc entries.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Meal entry created; Location header contains the URI",
                        content = @Content(schema = @Schema(implementation = MealEntryDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed or missing required fields"),
                @ApiResponse(responseCode = "404", description = "Referenced food not found")
            }
    )
    public Mono<ResponseEntity<MealEntryDTO>> addEntry(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date of the meal entry (ISO-8601)", example = "2026-06-07") LocalDate date,
            @Valid @RequestBody CreateMealEntryRequest req) {
        return mealEntryService.addEntry(date, req)
                .map(created -> {
                    final URI location = URI.create(ENTRIES_LOCATION_PREFIX + created.id());
                    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
                });
    }

    /**
     * Returns the nutritional summary for the given date, including all entries and totals vs. targets.
     *
     * @param date the date to summarise
     * @return a Mono emitting the day summary DTO with 200 OK
     */
    @GetMapping("/nutrition/days/{date}")
    @Operation(
            summary = "Get day summary",
            description = "Returns all meal entries for the given day together with macro totals and, "
                    + "if a nutrition profile and weight entry exist, targets and remaining macros.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Day summary returned",
                        content = @Content(schema = @Schema(implementation = DaySummaryDTO.class))
                )
            }
    )
    public Mono<DaySummaryDTO> getDay(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date to summarise (ISO-8601)", example = "2026-06-07") LocalDate date) {
        return mealEntryService.getDay(date);
    }

    /**
     * Updates the meal entry with the given id, or returns 404 if not found.
     *
     * @param id  the UUID of the entry to update
     * @param req the update request body
     * @return a Mono with 200 and the updated DTO, or 404 if not found
     */
    @PutMapping("/nutrition/entries/{id}")
    @Operation(
            summary = "Update a meal entry",
            description = "Applies partial updates to an existing meal entry. "
                    + "For food-backed entries a new quantityG triggers macro re-snapshotting. "
                    + "For ad-hoc entries, non-null macro values are applied directly.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Meal entry updated",
                        content = @Content(schema = @Schema(implementation = MealEntryDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Meal entry not found")
            }
    )
    public Mono<ResponseEntity<MealEntryDTO>> updateEntry(
            @PathVariable @Parameter(description = "UUID of the meal entry to update") UUID id,
            @Valid @RequestBody UpdateMealEntryRequest req) {
        return mealEntryService.updateEntry(id, req)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes the meal entry with the given id, or returns 404 if not found.
     *
     * @param id the UUID of the entry to delete
     * @return a Mono with 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/nutrition/entries/{id}")
    @Operation(
            summary = "Delete a meal entry",
            description = "Permanently removes the meal entry.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Meal entry deleted"),
                @ApiResponse(responseCode = "404", description = "Meal entry not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteEntry(
            @PathVariable @Parameter(description = "UUID of the meal entry to delete") UUID id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        return mealEntryService.deleteEntry(id)
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound);
    }
}
