package com.marvin.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Transient macro estimate for a described canteen meal.
 * This record is never persisted — it represents Claude's best-effort estimation
 * of the nutritional values for the described meal and portion.
 *
 * @param kcal        estimated kilocalories for the described portion
 * @param proteinG    estimated grams of protein
 * @param carbsG      estimated grams of carbohydrates
 * @param fatG        estimated grams of fat
 * @param assumptions a human-readable summary of assumptions Claude made during estimation
 */
@Schema(description = "Transient macro estimate for a described meal — never persisted")
@JsonIgnoreProperties(ignoreUnknown = true)
public record MealEstimateDTO(
        @Schema(description = "Estimated kilocalories for the described portion", example = "650.00")
        BigDecimal kcal,

        @Schema(description = "Estimated grams of protein", example = "45.00")
        BigDecimal proteinG,

        @Schema(description = "Estimated grams of carbohydrates", example = "70.00")
        BigDecimal carbsG,

        @Schema(description = "Estimated grams of fat", example = "18.00")
        BigDecimal fatG,

        @Schema(description = "Assumptions Claude made while estimating (e.g. portion size used)",
                example = "Estimated for a standard canteen portion of 400 g")
        String assumptions
) {
}
