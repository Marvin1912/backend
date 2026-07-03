package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for updating a meal-plan headline statistic.
 * All fields are optional; only non-null values are applied.
 *
 * @param label updated statistic label (nullable)
 * @param value updated statistic display value (nullable)
 */
@Schema(description = "Request to update a meal-plan headline statistic")
public record UpdateMealPlanStatRequest(
        @NullOrNotBlank
        @Schema(description = "Updated statistic label", example = "Tagesbudget (Ø)")
        String label,

        @NullOrNotBlank
        @Schema(description = "Updated statistic display value", example = "2.416 kcal")
        String value
) {
}
