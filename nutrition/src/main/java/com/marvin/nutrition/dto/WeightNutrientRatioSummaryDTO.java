package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object summarizing the average macro-nutrient-to-body-weight ratios over a
 * period, averaged only across days that have at least one logged meal entry.
 *
 * @param from             the first date of the period, or null if no meal entry was ever recorded
 * @param to               the last date of the period, or null if no meal entry was ever recorded
 * @param totalDays        the number of calendar days in the period
 * @param trackedDays      the number of days within the period that have at least one logged meal entry
 * @param avgProteinPerKg  the average grams of protein consumed per kilogram of body weight, over tracked
 *                         days, or null if not computable
 * @param avgCarbsPerKg    the average grams of carbohydrates consumed per kilogram of body weight, over
 *                         tracked days, or null if not computable
 * @param avgFatPerKg      the average grams of fat consumed per kilogram of body weight, over tracked days,
 *                         or null if not computable
 */
@Schema(description = "Average macro-nutrient-to-body-weight ratios over a period, averaged over tracked days")
public record WeightNutrientRatioSummaryDTO(
        @Schema(description = "First date of the period", example = "2026-07-13")
        LocalDate from,

        @Schema(description = "Last date of the period", example = "2026-08-11")
        LocalDate to,

        @Schema(description = "Number of calendar days in the period", example = "30")
        int totalDays,

        @Schema(description = "Number of days in the period with at least one logged meal entry", example = "24")
        int trackedDays,

        @Schema(description = "Average grams of protein consumed per kilogram of body weight", example = "2.10")
        BigDecimal avgProteinPerKg,

        @Schema(description = "Average grams of carbohydrates consumed per kilogram of body weight", example = "2.80")
        BigDecimal avgCarbsPerKg,

        @Schema(description = "Average grams of fat consumed per kilogram of body weight", example = "0.90")
        BigDecimal avgFatPerKg
) {
}
