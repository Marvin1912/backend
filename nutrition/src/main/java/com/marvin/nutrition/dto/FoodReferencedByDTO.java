package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object breaking down the references that prevent a food catalog entry's deletion.
 *
 * @param mealPlanRows      number of meal-plan rows referencing the food
 * @param mealTemplateItems number of meal-template items referencing the food
 */
@Schema(description = "Counts of the entities referencing a food catalog entry")
public record FoodReferencedByDTO(
        @Schema(description = "Number of meal-plan rows referencing the food", example = "2")
        long mealPlanRows,

        @Schema(description = "Number of meal-template items referencing the food", example = "1")
        long mealTemplateItems
) {
}
