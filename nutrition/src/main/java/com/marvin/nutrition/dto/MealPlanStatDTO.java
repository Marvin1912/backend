package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing a single headline statistic shown in the meal plan's summary bar.
 *
 * @param label the statistic's label
 * @param value the statistic's display value
 */
@Schema(description = "A headline statistic shown in the meal plan's summary bar")
public record MealPlanStatDTO(
        @Schema(description = "Statistic label", example = "Tagesbudget (Ø)")
        String label,

        @Schema(description = "Statistic display value", example = "2.416 kcal")
        String value
) {
}
