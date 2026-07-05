package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating a meal-plan section.
 * All fields are optional; only non-null values are applied.
 *
 * @param title     updated section title (nullable)
 * @param note      updated short note describing the section (nullable)
 * @param callout   updated explanatory callout text (nullable)
 * @param dayCount  updated number of calendar days per week this section applies to (nullable)
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

        @Positive
        @Schema(description = "Updated number of calendar days per week this section applies to", example = "4")
        Integer dayCount
) {

    /**
     * Preserves the pre-{@code dayCount} constructor signature for existing callers; omitting
     * {@code dayCount} means "leave unchanged", consistent with this record's partial-update
     * semantics for every other field.
     *
     * @param title   updated section title (nullable)
     * @param note    updated short note describing the section (nullable)
     * @param callout updated explanatory callout text (nullable)
     */
    public UpdateMealPlanSectionRequest(String title, String note, String callout) {
        this(title, note, callout, null);
    }
}
