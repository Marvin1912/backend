package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing a single food item with quantity within a meal template.
 *
 * @param id        server-assigned unique identifier of the template item
 * @param foodId    UUID of the referenced food item
 * @param foodName  resolved name of the referenced food item
 * @param quantityG portion size in grams
 * @param kcal      live-computed kilocalories for the given quantity, derived from the food catalog
 * @param proteinG  live-computed grams of protein for the given quantity, derived from the food catalog
 * @param carbsG    live-computed grams of carbohydrates for the given quantity, derived from the food catalog
 * @param fatG      live-computed grams of fat for the given quantity, derived from the food catalog
 */
@Schema(description = "A food item with quantity within a meal template")
public record MealTemplateItemDTO(
        @Schema(description = "Meal template item identifier")
        UUID id,

        @Schema(description = "Referenced food item UUID")
        UUID foodId,

        @Schema(description = "Resolved name of the referenced food item", example = "Oatmeal")
        String foodName,

        @Schema(description = "Portion size in grams", example = "50.00")
        BigDecimal quantityG,

        @Schema(description = "Live-computed kilocalories for this quantity", example = "185.00")
        BigDecimal kcal,

        @Schema(description = "Live-computed grams of protein for this quantity", example = "6.50")
        BigDecimal proteinG,

        @Schema(description = "Live-computed grams of carbohydrates for this quantity", example = "30.00")
        BigDecimal carbsG,

        @Schema(description = "Live-computed grams of fat for this quantity", example = "3.50")
        BigDecimal fatG
) {
}
