package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.CreateSportActivityRequest;
import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.dto.UpdateSportActivityRequest;
import com.marvin.nutrition.service.SportActivityService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for sport/exercise activity logging.
 * Provides endpoints to add, update and delete sport activities for a given day.
 */
@RestController
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class SportActivityController {

    private static final String ACTIVITIES_LOCATION_PREFIX = "/nutrition/activities/";

    private final SportActivityService sportActivityService;

    /**
     * Creates a new SportActivityController with the required service.
     *
     * @param sportActivityService the service handling sport activity operations
     */
    public SportActivityController(SportActivityService sportActivityService) {
        this.sportActivityService = sportActivityService;
    }

    /**
     * Logs a new sport activity for the given date and returns 201 Created with a Location header.
     *
     * @param date the date to log the activity for
     * @param req  the create request body
     * @return a Mono with 201 Created, Location header, and the created sport activity DTO
     */
    @PostMapping("/nutrition/days/{date}/activities")
    @Operation(
            summary = "Log a sport activity",
            description = "Adds a sport activity for the given day. "
                    + "When activityType is OTHER, a non-blank description is required.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Sport activity created; Location header contains the URI",
                        content = @Content(schema = @Schema(implementation = SportActivityDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed or missing required description")
            }
    )
    public Mono<ResponseEntity<SportActivityDTO>> addActivity(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date of the sport activity (ISO-8601)", example = "2026-06-07") LocalDate date,
            @Valid @RequestBody CreateSportActivityRequest req) {
        return sportActivityService.addActivity(date, req)
                .map(created -> {
                    final URI location = URI.create(ACTIVITIES_LOCATION_PREFIX + created.id());
                    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
                });
    }

    /**
     * Updates the sport activity with the given id, or returns 404 if not found.
     *
     * @param id  the UUID of the activity to update
     * @param req the update request body
     * @return a Mono with 200 and the updated DTO, 404 if not found, or 400 if invalid
     */
    @PutMapping("/nutrition/activities/{id}")
    @Operation(
            summary = "Update a sport activity",
            description = "Applies partial updates to an existing sport activity. "
                    + "If the resulting activityType is OTHER, a non-blank description is required.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Sport activity updated",
                        content = @Content(schema = @Schema(implementation = SportActivityDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed or missing required description"),
                @ApiResponse(responseCode = "404", description = "Sport activity not found")
            }
    )
    public Mono<ResponseEntity<SportActivityDTO>> updateActivity(
            @PathVariable @Parameter(description = "UUID of the sport activity to update") UUID id,
            @Valid @RequestBody UpdateSportActivityRequest req) {
        return sportActivityService.updateActivity(id, req)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes the sport activity with the given id, or returns 404 if not found.
     *
     * @param id the UUID of the activity to delete
     * @return a Mono with 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/nutrition/activities/{id}")
    @Operation(
            summary = "Delete a sport activity",
            description = "Permanently removes the sport activity.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Sport activity deleted"),
                @ApiResponse(responseCode = "404", description = "Sport activity not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteActivity(
            @PathVariable @Parameter(description = "UUID of the sport activity to delete") UUID id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        return sportActivityService.deleteActivity(id)
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound);
    }
}
