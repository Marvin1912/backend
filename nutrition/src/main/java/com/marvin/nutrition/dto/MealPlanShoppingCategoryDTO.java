package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a single category on the shopping list, e.g. "Fleisch & Fisch".
 *
 * @param id    the category's unique identifier
 * @param title the category's title
 * @param items the items in this category
 */
@Schema(description = "A single category on the shopping list")
public record MealPlanShoppingCategoryDTO(
        @Schema(description = "Category unique identifier")
        UUID id,

        @Schema(description = "Category title", example = "Fleisch & Fisch")
        String title,

        @Schema(description = "Items in this category")
        List<MealPlanShoppingItemDTO> items
) {
}
