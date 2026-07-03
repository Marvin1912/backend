package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Data Transfer Object representing the weekly shopping list.
 *
 * @param title      the shopping list's title
 * @param note       short note describing the shopping list's scope
 * @param categories the categories making up the shopping list
 * @param callout    optional explanatory callout text
 */
@Schema(description = "The weekly shopping list")
public record MealPlanShoppingListDTO(
        @Schema(description = "Shopping list title", example = "4 · Einkaufsliste für Lidl (1 Woche)")
        String title,

        @Schema(description = "Short note describing the shopping list's scope", example = "4× Kantine Mo–Do · 3× Selbstkochen Fr–So · 40 g Whey/Tag")
        String note,

        @Schema(description = "Categories making up the shopping list")
        List<MealPlanShoppingCategoryDTO> categories,

        @Schema(description = "Optional explanatory callout text")
        String callout
) {
}
