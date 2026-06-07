package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for estimating macros of a described canteen meal.
 * Supply a free-text {@code description} of the meal; optionally include a {@code portionHint}
 * to improve the accuracy of the calorie and macro estimate.
 *
 * @param description a free-text description of the meal (required, max 500 characters)
 * @param portionHint optional hint about the portion size (e.g. "one plate", "400 g")
 */
@Schema(description = "Request to estimate macros for a described meal using Claude")
public record MealEstimateRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "Free-text description of the meal to estimate", example = "Schnitzel mit Pommes")
        String description,

        @Size(max = 255)
        @Schema(description = "Optional portion hint to improve estimate accuracy", example = "one standard plate")
        String portionHint
) {
}
