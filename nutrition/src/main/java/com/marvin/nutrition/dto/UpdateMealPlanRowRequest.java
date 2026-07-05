package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for updating a meal-plan row. {@code mealType} is optional and only applied when
 * non-null; {@code foodId} and {@code quantityG} are always required and macros are re-snapshotted
 * from the referenced food's per-100g values on every update.
 *
 * @param mealType  updated meal category (nullable)
 * @param foodId    UUID of the food catalog item the row references (required)
 * @param quantityG updated portion size in grams; must be positive (required)
 */
@Schema(description = "Request to update a meal-plan row")
public record UpdateMealPlanRowRequest(
        @Schema(description = "Updated meal category (nullable)", example = "DINNER")
        MealType mealType,

        @NotNull
        @Schema(description = "Food catalog item UUID the row references", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID foodId,

        @NotNull
        @Positive
        @Schema(description = "Updated portion size in grams", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantityG
) {
}
