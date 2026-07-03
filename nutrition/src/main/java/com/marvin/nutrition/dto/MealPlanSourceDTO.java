package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing a single external data source referenced by the meal plan.
 *
 * @param label display label of the source
 * @param url   link to the source
 */
@Schema(description = "A single external data source referenced by the meal plan")
public record MealPlanSourceDTO(
        @Schema(description = "Display label of the source", example = "Magerquark (Milbona/Milsani, fatsecret.de)")
        String label,

        @Schema(description = "Link to the source", example = "https://www.fatsecret.de/Kalorien-Ern%C3%A4hrung/milsani/magerquark/100g")
        String url
) {
}
