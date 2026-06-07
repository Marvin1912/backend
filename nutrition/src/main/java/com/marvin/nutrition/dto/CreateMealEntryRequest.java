package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for creating a new meal log entry.
 * Supply {@code foodId} for food-backed entries; omit it for ad-hoc entries.
 * Food-backed entries require {@code quantityG}; macros are then snapshotted from the food catalog.
 * Ad-hoc entries require {@code description} and all four macro fields.
 *
 * @param mealType    the meal category (required)
 * @param foodId      UUID of the food catalog item (nullable — omit for ad-hoc entries)
 * @param quantityG   portion size in grams (required for food-backed entries)
 * @param description free-text label (required for ad-hoc entries)
 * @param kcal        kilocalories (required for ad-hoc entries)
 * @param proteinG    grams of protein (required for ad-hoc entries)
 * @param carbsG      grams of carbohydrates (required for ad-hoc entries)
 * @param fatG        grams of fat (required for ad-hoc entries)
 */
@Schema(description = "Request to log a meal entry for a given day")
public record CreateMealEntryRequest(
        @NotNull
        @Schema(description = "Meal category", example = "LUNCH")
        MealType mealType,

        @Schema(description = "Food catalog item UUID; null for ad-hoc entries")
        UUID foodId,

        @Schema(description = "Portion size in grams; required for food-backed entries", example = "150.00")
        BigDecimal quantityG,

        @Schema(description = "Free-text description; required for ad-hoc entries", example = "Homemade soup")
        String description,

        @Schema(description = "Kilocalories; required for ad-hoc entries", example = "250.00")
        BigDecimal kcal,

        @Schema(description = "Grams of protein; required for ad-hoc entries", example = "15.00")
        BigDecimal proteinG,

        @Schema(description = "Grams of carbohydrates; required for ad-hoc entries", example = "30.00")
        BigDecimal carbsG,

        @Schema(description = "Grams of fat; required for ad-hoc entries", example = "8.00")
        BigDecimal fatG
) {
}
