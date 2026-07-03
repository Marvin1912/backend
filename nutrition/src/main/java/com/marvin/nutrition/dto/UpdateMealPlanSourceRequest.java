package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for updating a meal-plan footer source.
 * All fields are optional; only non-null values are applied.
 *
 * @param label updated display label of the source (nullable)
 * @param url   updated link to the source (nullable)
 */
@Schema(description = "Request to update a meal-plan footer source")
public record UpdateMealPlanSourceRequest(
        @NullOrNotBlank
        @Schema(description = "Updated display label of the source", example = "Magerquark (Milbona/Milsani, fatsecret.de)")
        String label,

        @NullOrNotBlank
        @Schema(description = "Updated link to the source", example = "https://www.fatsecret.de/magerquark")
        String url
) {
}
