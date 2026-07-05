package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for creating a new meal-plan row within a section. Macros are derived server-side
 * from the referenced food's per-100g values and the supplied quantity.
 *
 * @param mealType  the meal category (required)
 * @param foodId    UUID of the food catalog item the row references (required)
 * @param quantityG portion size in grams; must be positive (required)
 */
@Schema(description = "Request to create a meal-plan row within a section")
public record CreateMealPlanRowRequest(
        @NotNull
        @Schema(description = "Meal category", example = "BREAKFAST", requiredMode = Schema.RequiredMode.REQUIRED)
        MealType mealType,

        @NotNull
        @Schema(description = "Food catalog item UUID the row references", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID foodId,

        @NotNull
        @Positive
        @Schema(description = "Portion size in grams", example = "90.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantityG
) {
}
