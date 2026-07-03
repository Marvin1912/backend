package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for updating a meal-plan row.
 * All fields are optional; only non-null values are applied.
 *
 * @param meal    updated meal name (nullable)
 * @param details updated description of the meal's ingredients (nullable)
 * @param qty     updated quantities of the ingredients as a display string (nullable)
 * @param kcal    updated kilocalories as a display string (nullable)
 * @param protein updated grams of protein as a display string (nullable)
 */
@Schema(description = "Request to update a meal-plan row")
public record UpdateMealPlanRowRequest(
        @NullOrNotBlank
        @Schema(description = "Updated meal name", example = "Frühstück")
        String meal,

        @NullOrNotBlank
        @Schema(description = "Updated description of the meal's ingredients")
        String details,

        @NullOrNotBlank
        @Schema(description = "Updated quantities of the ingredients as a display string", example = "90g/200ml/20g/100g/50g/10g/1 Stk")
        String qty,

        @NullOrNotBlank
        @Schema(description = "Updated kilocalories as a display string", example = "663")
        String kcal,

        @NullOrNotBlank
        @Schema(description = "Updated grams of protein as a display string", example = "46,5 g")
        String protein
) {
}
