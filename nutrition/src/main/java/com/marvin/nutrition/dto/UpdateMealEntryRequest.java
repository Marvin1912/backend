package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body for updating an existing meal log entry.
 * All fields are optional; only non-null values are applied.
 * For food-backed entries a new {@code quantityG} triggers macro re-snapshotting.
 * For ad-hoc entries the supplied macro values and description are applied directly.
 *
 * @param mealType    updated meal category (nullable)
 * @param quantityG   updated portion size in grams; triggers re-snapshot for food-backed entries (nullable)
 * @param description updated free-text label for ad-hoc entries (nullable)
 * @param kcal        updated kilocalories for ad-hoc entries (nullable)
 * @param proteinG    updated grams of protein for ad-hoc entries (nullable)
 * @param carbsG      updated grams of carbohydrates for ad-hoc entries (nullable)
 * @param fatG        updated grams of fat for ad-hoc entries (nullable)
 */
@Schema(description = "Request to update an existing meal log entry")
public record UpdateMealEntryRequest(
        @Schema(description = "Updated meal category (nullable)", example = "DINNER")
        MealType mealType,

        @PositiveOrZero
        @Schema(description = "Updated portion size in grams; triggers macro re-snapshot for food-backed entries", example = "200.00")
        BigDecimal quantityG,

        @Size(max = 255)
        @Schema(description = "Updated free-text description for ad-hoc entries", example = "Updated soup")
        String description,

        @PositiveOrZero
        @Schema(description = "Updated kilocalories for ad-hoc entries", example = "350.00")
        BigDecimal kcal,

        @PositiveOrZero
        @Schema(description = "Updated grams of protein for ad-hoc entries", example = "20.00")
        BigDecimal proteinG,

        @PositiveOrZero
        @Schema(description = "Updated grams of carbohydrates for ad-hoc entries", example = "40.00")
        BigDecimal carbsG,

        @PositiveOrZero
        @Schema(description = "Updated grams of fat for ad-hoc entries", example = "10.00")
        BigDecimal fatG
) {
}
