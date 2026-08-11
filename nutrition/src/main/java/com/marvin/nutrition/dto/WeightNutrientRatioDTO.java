package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object pairing a day's total nutrient intake with the applicable historical body
 * weight and the resulting per-kilogram ratios.
 *
 * @param date          the day this entry applies to
 * @param weightKg      the applicable body weight in kilograms, or null if no weight entry exists at all
 * @param kcal          total kilocalories consumed on this day
 * @param proteinG      total grams of protein consumed on this day
 * @param carbsG        total grams of carbohydrates consumed on this day
 * @param fatG          total grams of fat consumed on this day
 * @param kcalPerKg     kilocalories consumed per kilogram of body weight, or null if weightKg is null
 * @param proteinPerKg  grams of protein consumed per kilogram of body weight, or null if weightKg is null
 * @param carbsPerKg    grams of carbohydrates consumed per kilogram of body weight, or null if weightKg is null
 * @param fatPerKg      grams of fat consumed per kilogram of body weight, or null if weightKg is null
 */
@Schema(description = "A day's total nutrient intake paired with the applicable body weight and per-kilogram ratios")
public record WeightNutrientRatioDTO(
        @Schema(description = "The day this entry applies to", example = "2024-06-01")
        LocalDate date,

        @Schema(description = "Applicable body weight in kilograms", example = "80.50")
        BigDecimal weightKg,

        @Schema(description = "Total kilocalories consumed on this day", example = "2200.00")
        BigDecimal kcal,

        @Schema(description = "Total grams of protein consumed on this day", example = "160.00")
        BigDecimal proteinG,

        @Schema(description = "Total grams of carbohydrates consumed on this day", example = "220.00")
        BigDecimal carbsG,

        @Schema(description = "Total grams of fat consumed on this day", example = "70.00")
        BigDecimal fatG,

        @Schema(description = "Kilocalories consumed per kilogram of body weight", example = "27.33")
        BigDecimal kcalPerKg,

        @Schema(description = "Grams of protein consumed per kilogram of body weight", example = "2.00")
        BigDecimal proteinPerKg,

        @Schema(description = "Grams of carbohydrates consumed per kilogram of body weight", example = "2.73")
        BigDecimal carbsPerKg,

        @Schema(description = "Grams of fat consumed per kilogram of body weight", example = "0.87")
        BigDecimal fatPerKg
) {
}
