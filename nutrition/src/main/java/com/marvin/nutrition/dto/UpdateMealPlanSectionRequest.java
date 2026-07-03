package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating a meal-plan section.
 * All fields are optional; only non-null values are applied.
 *
 * @param title         updated section title (nullable)
 * @param note          updated short note describing the section (nullable)
 * @param callout       updated explanatory callout text (nullable)
 * @param totalsLabel   updated totals row label (nullable)
 * @param totalsKcal    updated totals row kcal display value (nullable)
 * @param totalsProtein updated totals row protein display value (nullable)
 */
@Schema(description = "Request to update a meal-plan section")
public record UpdateMealPlanSectionRequest(
        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Updated section title", example = "1 · Tagesstruktur (täglich gleich)")
        String title,

        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Updated short note describing the section")
        String note,

        @NullOrNotBlank
        @Schema(description = "Updated explanatory callout text")
        String callout,

        @NullOrNotBlank
        @Size(max = 255)
        @Schema(description = "Updated totals row label", example = "Tagesgesamt")
        String totalsLabel,

        @NullOrNotBlank
        @Size(max = 255)
        @Schema(description = "Updated totals row kcal display value", example = "2.407 kcal")
        String totalsKcal,

        @NullOrNotBlank
        @Size(max = 255)
        @Schema(description = "Updated totals row protein display value", example = "182,2 g")
        String totalsProtein
) {

    /**
     * Creates an update request without any totals-row changes; {@code totalsLabel}, {@code totalsKcal}
     * and {@code totalsProtein} are left {@code null}, leaving the section's totals row unchanged.
     *
     * @param title   updated section title (nullable)
     * @param note    updated short note describing the section (nullable)
     * @param callout updated explanatory callout text (nullable)
     */
    public UpdateMealPlanSectionRequest(String title, String note, String callout) {
        this(title, note, callout, null, null, null);
    }
}
