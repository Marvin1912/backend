package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request entry describing a single food item with quantity to include in a meal template.
 *
 * @param foodId    UUID of the food catalog item (required)
 * @param quantityG portion size in grams (required, must be positive)
 */
@Schema(description = "A food item with quantity to include in a meal template")
public record MealTemplateItemRequest(
        @NotNull
        @Schema(description = "Food catalog item UUID")
        UUID foodId,

        @NotNull
        @Positive
        @Schema(description = "Portion size in grams", example = "50.00")
        BigDecimal quantityG
) {
}
