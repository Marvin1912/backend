package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating the meal plan's header content.
 * All fields are optional; only non-null values are applied.
 *
 * @param eyebrow     updated small label shown above the title (nullable)
 * @param title       updated document title (nullable)
 * @param description updated short description of the plan's goal and approach (nullable)
 * @param footerNote  updated closing note (nullable)
 */
@Schema(description = "Request to update the meal plan's header content")
public record UpdateMealPlanRequest(
        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Updated small label shown above the title", example = "Version 3 — abgeglichen mit Tracking")
        String eyebrow,

        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Updated document title", example = "Ernährungsplan & Einkaufsliste")
        String title,

        @NullOrNotBlank
        @Schema(description = "Updated short description of the plan's goal and approach")
        String description,

        @NullOrNotBlank
        @Schema(description = "Updated closing note explaining where the nutritional data comes from")
        String footerNote
) {
}
