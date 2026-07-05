package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing the meal plan's header content, returned after updating it.
 *
 * @param eyebrow     small label shown above the title
 * @param title       document title
 * @param description short description of the plan's goal and approach
 * @param footerNote  closing note explaining where the nutritional data comes from
 */
@Schema(description = "The meal plan's header content")
public record MealPlanHeaderDTO(
        @Schema(description = "Small label shown above the title", example = "Version 2 — abgeglichen mit Tracking & Lebensmitteldatenbank")
        String eyebrow,

        @Schema(description = "Document title", example = "Ernährungsplan & Einkaufsliste")
        String title,

        @Schema(description = "Short description of the plan's goal and approach")
        String description,

        @Schema(description = "Closing note explaining where the nutritional data comes from")
        String footerNote
) {
}
