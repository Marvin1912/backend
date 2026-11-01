package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.ExcludeDayRequest;
import com.marvin.nutrition.dto.ExcludedDayDTO;
import com.marvin.nutrition.service.ExcludedDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing days excluded from nutrition evaluations, e.g. days spent eating
 * out where tracking is not meaningful.
 */
@RestController
@RequestMapping("/nutrition/excluded-days")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class ExcludedDayController {

    private final ExcludedDayService excludedDayService;

    /**
     * Creates a new ExcludedDayController.
     *
     * @param excludedDayService the service managing excluded days
     */
    public ExcludedDayController(ExcludedDayService excludedDayService) {
        this.excludedDayService = excludedDayService;
    }

    /**
     * Returns all excluded days within the given range (inclusive).
     *
     * @param from the first date to include (inclusive)
     * @param to   the last date to include (inclusive)
     * @return a Mono emitting the excluded days, ascending by date
     */
    @GetMapping
    @Operation(
            summary = "List excluded days",
            description = "Returns all days within the given range that are excluded from nutrition evaluations.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Excluded days returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExcludedDayDTO.class)))
                )
            }
    )
    public Mono<List<ExcludedDayDTO>> getExcludedDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "First date to include (inclusive)") LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Last date to include (inclusive)") LocalDate to) {
        return excludedDayService.findBetween(from, to);
    }

    /**
     * Excludes the given day from nutrition evaluations, or updates its reason if already excluded.
     *
     * @param date    the day to exclude
     * @param request optional reason for the exclusion
     * @return a Mono with 200 and the excluded day
     */
    @PutMapping("/{date}")
    @Operation(
            summary = "Exclude a day",
            description = "Marks the given day as excluded from averages, ratios and adherence statistics. "
                    + "Idempotent; updates the reason if the day is already excluded.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Day excluded",
                        content = @Content(schema = @Schema(implementation = ExcludedDayDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed")
            }
    )
    public Mono<ResponseEntity<ExcludedDayDTO>> excludeDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Day to exclude") LocalDate date,
            @Valid @RequestBody ExcludeDayRequest request) {
        return excludedDayService.exclude(date, request.reason()).map(ResponseEntity::ok);
    }

    /**
     * Re-includes the given day in nutrition evaluations.
     *
     * @param date the day to include again
     * @return a Mono with 204 No Content
     */
    @DeleteMapping("/{date}")
    @Operation(
            summary = "Include a day again",
            description = "Removes the exclusion of the given day. Idempotent.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Day included again")
            }
    )
    public Mono<ResponseEntity<Void>> includeDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Day to include again") LocalDate date) {
        return excludedDayService.include(date).thenReturn(ResponseEntity.<Void>noContent().build());
    }
}
