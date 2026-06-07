package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object representing the complete nutritional summary for a given day.
 *
 * @param date      the date of the summary
 * @param entries   all meal entries logged for this day
 * @param totals    aggregated macro-nutrient totals across all entries
 * @param targets   computed daily nutrition targets (null if profile or weight data is missing)
 * @param remaining remaining macro budget (targets minus totals; null if targets are unavailable)
 */
@Schema(description = "Nutritional summary for a given day")
public record DaySummaryDTO(
        @Schema(description = "Date of the summary", example = "2026-06-07")
        LocalDate date,

        @Schema(description = "All meal entries logged for this day")
        List<MealEntryDTO> entries,

        @Schema(description = "Aggregated macro-nutrient totals across all entries")
        MacrosDTO totals,

        @Schema(description = "Computed daily nutrition targets; null if profile or weight data is missing")
        TargetsDTO targets,

        @Schema(description = "Remaining macro budget (targets minus totals); null if targets are unavailable")
        MacrosDTO remaining
) {
}
