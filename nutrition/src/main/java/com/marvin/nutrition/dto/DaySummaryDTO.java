package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object representing the complete nutritional summary for a given day.
 *
 * @param date            the date of the summary
 * @param entries         all meal entries logged for this day
 * @param totals          aggregated macro-nutrient totals across all entries
 * @param targets         the nutrition targets that applied on this day (null if profile or weight data is
 *                        missing). If a target snapshot was recorded for this day (created the first time a
 *                        meal entry was logged for it), that historical snapshot is returned so past days keep
 *                        showing the targets that applied at the time, even if the profile or weight has since
 *                        changed. Otherwise, the currently computed (live) targets are returned.
 * @param remaining       remaining macro budget (targets minus totals, with burned kcal added back to the kcal
 *                        line; null if targets are unavailable)
 * @param activities      all sport activities logged for this day
 * @param totalKcalBurned aggregated kilocalories burned across all activities for this day
 */
@Schema(description = "Nutritional summary for a given day")
public record DaySummaryDTO(
        @Schema(description = "Date of the summary", example = "2026-06-07")
        LocalDate date,

        @Schema(description = "All meal entries logged for this day")
        List<MealEntryDTO> entries,

        @Schema(description = "Aggregated macro-nutrient totals across all entries")
        MacrosDTO totals,

        @Schema(description = "Nutrition targets that applied on this day "
                + "(a historical snapshot if one was recorded, otherwise live targets); "
                + "null if profile or weight data is missing")
        TargetsDTO targets,

        @Schema(description = "Remaining macro budget (targets minus totals, with burned kcal added back to "
                + "the kcal line); null if targets are unavailable")
        MacrosDTO remaining,

        @Schema(description = "All sport activities logged for this day")
        List<SportActivityDTO> activities,

        @Schema(description = "Total kilocalories burned across all activities for this day", example = "300.00")
        BigDecimal totalKcalBurned
) {
}
