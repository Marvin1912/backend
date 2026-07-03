package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing a single meal row within a meal plan section.
 *
 * @param meal    the meal's name
 * @param details description of the meal's ingredients
 * @param qty     quantities of the ingredients as a display string
 * @param kcal    kilocalories as a display string
 * @param protein grams of protein as a display string
 */
@Schema(description = "A single meal row within a meal plan section")
public record MealPlanRowDTO(
        @Schema(description = "Meal name", example = "Frühstück")
        String meal,

        @Schema(description = "Description of the meal's ingredients")
        String details,

        @Schema(description = "Quantities of the ingredients as a display string", example = "90g/200ml/20g/100g/50g/10g/1 Stk")
        String qty,

        @Schema(description = "Kilocalories as a display string", example = "663")
        String kcal,

        @Schema(description = "Grams of protein as a display string", example = "46,5 g")
        String protein
) {
}
