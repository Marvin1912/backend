package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing a single food-backed meal row within a meal plan section.
 *
 * @param id        the row's unique identifier
 * @param mealType  the meal category (BREAKFAST, LUNCH, DINNER, SNACK)
 * @param foodId    UUID of the referenced food catalog item
 * @param foodName  snapshot of the food's name at write time
 * @param quantityG portion size in grams
 * @param kcal      snapshotted kilocalories
 * @param proteinG  snapshotted grams of protein
 * @param carbsG    snapshotted grams of carbohydrates
 * @param fatG      snapshotted grams of fat
 */
@Schema(description = "A single food-backed meal row within a meal plan section")
public record MealPlanRowDTO(
        @Schema(description = "Row unique identifier")
        UUID id,

        @Schema(description = "Meal category", example = "BREAKFAST")
        MealType mealType,

        @Schema(description = "Referenced food catalog item UUID")
        UUID foodId,

        @Schema(description = "Snapshot of the food's name at write time", example = "Haferflocken")
        String foodName,

        @Schema(description = "Portion size in grams", example = "90.00")
        BigDecimal quantityG,

        @Schema(description = "Snapshotted kilocalories", example = "519.00")
        BigDecimal kcal,

        @Schema(description = "Snapshotted grams of protein", example = "28.00")
        BigDecimal proteinG,

        @Schema(description = "Snapshotted grams of carbohydrates", example = "60.00")
        BigDecimal carbsG,

        @Schema(description = "Snapshotted grams of fat", example = "10.00")
        BigDecimal fatG
) {
}
