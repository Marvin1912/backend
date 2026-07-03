package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Data Transfer Object representing the static weekly meal-plan reference document.
 *
 * @param eyebrow      small label shown above the title
 * @param title        document title
 * @param description  short description of the plan's goal and approach
 * @param stats        headline daily budget statistics
 * @param changelog    list of changes compared to the previous plan version
 * @param sections     the plan's daily/weekly meal sections
 * @param shoppingList the weekly shopping list
 * @param footer       closing note and data sources
 */
@Schema(description = "The static weekly meal-plan reference document")
public record MealPlanDTO(
        @Schema(description = "Small label shown above the title", example = "Version 2 — abgeglichen mit Tracking & Lebensmitteldatenbank")
        String eyebrow,

        @Schema(description = "Document title", example = "Ernährungsplan & Einkaufsliste")
        String title,

        @Schema(description = "Short description of the plan's goal and approach")
        String description,

        @Schema(description = "Headline daily budget statistics")
        List<MealPlanStatDTO> stats,

        @Schema(description = "Changes compared to the previous plan version")
        List<MealPlanChangelogEntryDTO> changelog,

        @Schema(description = "The plan's daily/weekly meal sections")
        List<MealPlanSectionDTO> sections,

        @Schema(description = "The weekly shopping list")
        MealPlanShoppingListDTO shoppingList,

        @Schema(description = "Closing note and data sources")
        MealPlanFooterDTO footer
) {
}
