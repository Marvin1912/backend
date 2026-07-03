package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing the totals row of a meal plan section.
 *
 * @param label   the totals row's label
 * @param kcal    total kilocalories as a display string
 * @param protein total grams of protein as a display string
 */
@Schema(description = "The totals row of a meal plan section")
public record MealPlanTotalsDTO(
        @Schema(description = "Totals row label", example = "Tagesgesamt")
        String label,

        @Schema(description = "Total kilocalories as a display string", example = "2.407 kcal")
        String kcal,

        @Schema(description = "Total grams of protein as a display string", example = "182,2 g")
        String protein
) {
}
