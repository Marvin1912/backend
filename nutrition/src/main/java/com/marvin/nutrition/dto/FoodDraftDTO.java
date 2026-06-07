package com.marvin.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Transient draft food sourced from either a Claude nutrition-label scan or an OpenFoodFacts
 * barcode lookup. This record is never persisted — it carries raw per-100 g values and an
 * optional serving size until the caller decides whether to save a {@code FoodEntity}.
 *
 * @param name          food product name
 * @param brand         optional brand name; null if not available from the source
 * @param kcalPer100    kilocalories per 100 g
 * @param proteinPer100 grams of protein per 100 g
 * @param carbsPer100   grams of carbohydrates per 100 g
 * @param fatPer100     grams of fat per 100 g
 * @param fiberPer100   grams of dietary fibre per 100 g; null if not provided by the source
 * @param servingG      suggested serving size in grams; null if not provided by the source
 */
@Schema(description = "Transient food draft from a label scan or barcode lookup — never persisted")
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
