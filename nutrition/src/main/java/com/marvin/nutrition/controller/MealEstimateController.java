package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.MealEstimateDTO;
import com.marvin.nutrition.dto.MealEstimateRequest;
import com.marvin.nutrition.service.MealEstimator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller that estimates macros for a described canteen meal via Claude.
 * The result is transient — nothing is persisted by this endpoint.
 */
@RestController
@RequestMapping("/nutrition/estimate")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class MealEstimateController {

    private final MealEstimator mealEstimator;

    /**
     * Creates a new MealEstimateController.
     *
     * @param mealEstimator the service that estimates meal macros via Claude
     */
    public MealEstimateController(MealEstimator mealEstimator) {
        this.mealEstimator = mealEstimator;
    }

    /**
     * Accepts a meal description, calls Claude to estimate macros, and returns the estimate.
     * Nothing is persisted — the returned estimate is only held in memory.
     *
     * @param request the meal description and optional portion hint
     * @return a Mono with 200 OK and the parsed macro estimate DTO
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Estimate macros for a described meal",
            description = "Sends the meal description to Claude and returns a transient macro estimate. "
                    + "Provide an optional portion hint to improve accuracy. The estimate is never persisted.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Macro estimate successfully returned",
                        content = @Content(schema = @Schema(implementation = MealEstimateDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed — description is blank or too long"),
                @ApiResponse(responseCode = "422", description = "Estimation failed — Claude could not produce a usable result")
            }
    )
    public Mono<ResponseEntity<MealEstimateDTO>> estimate(@Valid @RequestBody MealEstimateRequest request) {
        return mealEstimator.estimate(request.description(), request.portionHint())
                .map(ResponseEntity::ok);
    }
}
