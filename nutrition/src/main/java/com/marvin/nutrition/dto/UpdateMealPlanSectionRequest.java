package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for updating a meal-plan section.
 * All fields are optional; only non-null values are applied.
 *
 * @param title   updated section title (nullable)
 * @param note    updated short note describing the section (nullable)
 * @param callout updated explanatory callout text (nullable)
 */
@Schema(description = "Request to update a meal-plan section")
public record UpdateMealPlanSectionRequest(
        @NullOrNotBlank
        @Schema(description = "Updated section title", example = "1 · Tagesstruktur (täglich gleich)")
        String title,

        @NullOrNotBlank
        @Schema(description = "Updated short note describing the section")
        String note,

        @NullOrNotBlank
        @Schema(description = "Updated explanatory callout text")
        String callout
) {
}
