package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object bundling the weight/nutrient-ratio summary for the last 30 days alongside
 * the summary for the entire tracked period.
 *
 * @param last30Days  the average macro-to-body-weight ratios over the last 30 days (inclusive of today)
 * @param totalPeriod the average macro-to-body-weight ratios over the entire tracked period
 */
@Schema(description = "Weight/nutrient-ratio summaries for the last 30 days and the entire tracked period")
public record WeightNutrientRatioSummaryResponse(
        @Schema(description = "Summary for the last 30 days, inclusive of today")
        WeightNutrientRatioSummaryDTO last30Days,

        @Schema(description = "Summary for the entire tracked period")
        WeightNutrientRatioSummaryDTO totalPeriod
) {
}
