package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating a meal-plan shopping-list category.
 * All fields are optional; only non-null values are applied.
 *
 * @param title updated category title (nullable)
 */
@Schema(description = "Request to update a meal-plan shopping-list category")
public record UpdateMealPlanShoppingCategoryRequest(
        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Updated category title", example = "Fleisch & Fisch")
        String title
) {
}
