package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing a logged meal entry.
 *
 * @param id          server-assigned unique identifier
 * @param entryDate   the date this entry belongs to
 * @param mealType    the meal category (BREAKFAST, LUNCH, DINNER, SNACK)
 * @param foodId      UUID of the referenced food item (null for ad-hoc entries)
 * @param description free-text label for ad-hoc entries (null for food-backed entries)
 * @param quantityG   portion size in grams (null for ad-hoc entries)
 * @param kcal        snapshotted kilocalories
 * @param proteinG    snapshotted grams of protein
 * @param carbsG      snapshotted grams of carbohydrates
 * @param fatG        snapshotted grams of fat
 * @param foodName    resolved name of the referenced food item; uses the live catalog name if the food
 *                     still exists, otherwise falls back to the snapshotted name (null for ad-hoc entries)
 */
@Schema(description = "A logged meal entry for a given day")
public record MealEntryDTO(
        @Schema(description = "Meal entry identifier")
        UUID id,

        @Schema(description = "Date of the entry", example = "2026-06-07")
        LocalDate entryDate,

        @Schema(description = "Meal category", example = "LUNCH")
        MealType mealType,

        @Schema(description = "Referenced food item UUID (null for ad-hoc entries)")
        UUID foodId,

        @Schema(description = "Free-text description for ad-hoc entries", example = "Homemade soup")
        String description,

        @Schema(description = "Portion size in grams (null for ad-hoc entries)", example = "150.00")
        BigDecimal quantityG,

        @Schema(description = "Snapshotted kilocalories", example = "300.00")
        BigDecimal kcal,

        @Schema(description = "Snapshotted grams of protein", example = "30.00")
        BigDecimal proteinG,

        @Schema(description = "Snapshotted grams of carbohydrates", example = "15.00")
        BigDecimal carbsG,

        @Schema(description = "Snapshotted grams of fat", example = "7.50")
        BigDecimal fatG,

        @Schema(description = "Resolved name of the referenced food item (null for ad-hoc entries)", example = "Chicken Breast")
        String foodName
) {
}
