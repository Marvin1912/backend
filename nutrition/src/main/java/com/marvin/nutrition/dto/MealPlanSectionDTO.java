package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a single section of the meal plan, e.g. a daily structure or a
 * group of weekdays sharing the same meals.
 *
 * @param id      the section's unique identifier
 * @param title   the section's title
 * @param note    short note describing the section
 * @param rows    the meal rows making up this section
 * @param totals  the section's totals row, or {@code null} if not applicable
 * @param callout optional explanatory callout text, or {@code null} if none
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

        @Schema(description = "Section totals row, absent if not applicable")
        MealPlanTotalsDTO totals,

        @Schema(description = "Optional explanatory callout text, absent if none")
        String callout
) {
}
