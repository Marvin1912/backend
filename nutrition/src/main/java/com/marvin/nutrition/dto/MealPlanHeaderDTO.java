package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing the meal plan's header content, returned after updating it.
 *
 * @param eyebrow             small label shown above the title
 * @param title               document title
 * @param description         short description of the plan's goal and approach
 * @param shoppingListTitle   the shopping list's title
 * @param shoppingListNote    short note describing the shopping list's scope
 * @param shoppingListCallout optional explanatory callout text for the shopping list, or {@code null} if none
 * @param footerNote          closing note explaining where the nutritional data comes from
 */
@Schema(description = "The meal plan's header content")
public record MealPlanHeaderDTO(
        @Schema(description = "Small label shown above the title", example = "Version 2 — abgeglichen mit Tracking & Lebensmitteldatenbank")
        String eyebrow,

        @Schema(description = "Document title", example = "Ernährungsplan & Einkaufsliste")
        String title,

        @Schema(description = "Short description of the plan's goal and approach")
        String description,

        @Schema(description = "Shopping list title", example = "4 · Einkaufsliste für Lidl (1 Woche)")
        String shoppingListTitle,

        @Schema(description = "Short note describing the shopping list's scope")
        String shoppingListNote,

        @Schema(description = "Optional explanatory callout text for the shopping list, absent if none")
        String shoppingListCallout,

        @Schema(description = "Closing note explaining where the nutritional data comes from")
        String footerNote
) {
}
