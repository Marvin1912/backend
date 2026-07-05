package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a single section of the meal plan, e.g. a daily structure or a
 * group of weekdays sharing the same meals.
 *
 * @param id        the section's unique identifier
 * @param title     the section's title
 * @param note      short note describing the section
 * @param rows      the meal rows making up this section
 * @param callout   optional explanatory callout text, or {@code null} if none
 * @param dayCount  number of calendar days per week this section applies to, e.g. 4 for a
 *                  Monday-Thursday block; used to scale row quantities when aggregating a
 *                  shopping list across the week
 */
@Schema(description = "A single section of the meal plan")
public record MealPlanSectionDTO(
        @Schema(description = "Section unique identifier")
        UUID id,

        @Schema(description = "Section title", example = "1 · Tagesstruktur (täglich gleich)")
        String title,

        @Schema(description = "Short note describing the section", example = "Frühstück & Nachmittag identisch an allen 7 Tagen")
        String note,

        @Schema(description = "Meal rows making up this section")
        List<MealPlanRowDTO> rows,

        @Schema(description = "Optional explanatory callout text, absent if none")
        String callout,

        @Schema(description = "Number of calendar days per week this section applies to", example = "4")
        Integer dayCount
) {

    /**
     * Preserves the pre-{@code dayCount} constructor signature for existing callers, defaulting
     * {@code dayCount} to {@code 1} (a single day), matching the entity's own default.
     *
     * @param id      the section's unique identifier
     * @param title   the section's title
     * @param note    short note describing the section
     * @param rows    the meal rows making up this section
     * @param callout optional explanatory callout text, or {@code null} if none
     */
    public MealPlanSectionDTO(UUID id, String title, String note, List<MealPlanRowDTO> rows, String callout) {
        this(id, title, note, rows, callout, 1);
    }
}
