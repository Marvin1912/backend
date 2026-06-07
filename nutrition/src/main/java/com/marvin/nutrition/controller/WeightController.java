package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.CreateWeightEntryRequest;
import com.marvin.nutrition.dto.WeightEntryDTO;
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
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /**
     * Creates a new WeightController.
     *
     * @param weightService the service handling weight entry operations
     */
    public WeightController(WeightService weightService) {
        this.weightService = weightService;
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
}
