package com.marvin.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Transient draft food parsed from a nutrition label photo.
 * This record is never persisted — it represents raw values read by Claude
 * directly from a packaged food's nutrition label image.
 *
 * @param name          food product name
 * @param brand         optional brand name; null if not visible on label
 * @param kcalPer100    kilocalories per 100 g, normalised from the label
 * @param proteinPer100 grams of protein per 100 g, normalised from the label
 * @param carbsPer100   grams of carbohydrates per 100 g, normalised from the label
 * @param fatPer100     grams of fat per 100 g, normalised from the label
 * @param fiberPer100   grams of dietary fibre per 100 g; null if not present on label
 * @param servingG      suggested serving size in grams; null if not present on label
 */
@Schema(description = "Transient food draft parsed from a nutrition label photo — never persisted")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodDraftDTO(
        @Schema(description = "Food product name", example = "Müsli")
        String name,

        @Schema(description = "Brand name (nullable)", example = "Kellogg's")
        String brand,

        @Schema(description = "Kilocalories per 100 g, normalised", example = "370.00")
        BigDecimal kcalPer100,

        @Schema(description = "Grams of protein per 100 g, normalised", example = "8.50")
        BigDecimal proteinPer100,

        @Schema(description = "Grams of carbohydrates per 100 g, normalised", example = "67.00")
        BigDecimal carbsPer100,

        @Schema(description = "Grams of fat per 100 g, normalised", example = "6.00")
        BigDecimal fatPer100,

        @Schema(description = "Grams of dietary fibre per 100 g (nullable)", example = "5.50")
        BigDecimal fiberPer100,

        @Schema(description = "Suggested serving size in grams (nullable)", example = "45.00")
        BigDecimal servingG
) {
}
