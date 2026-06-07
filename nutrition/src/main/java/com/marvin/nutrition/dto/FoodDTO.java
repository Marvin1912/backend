package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.FoodSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing a food item in the nutrition catalog.
 *
 * @param id              server-assigned unique identifier
 * @param name            food name (required)
 * @param brand           optional brand name
 * @param kcalPer100      kilocalories per 100 g (required, non-negative)
 * @param proteinPer100   grams of protein per 100 g (required, non-negative)
 * @param carbsPer100     grams of carbohydrates per 100 g (required, non-negative)
 * @param fatPer100       grams of fat per 100 g (required, non-negative)
 * @param fiberPer100     grams of dietary fibre per 100 g (optional, non-negative if present)
 * @param defaultServingG default serving size in grams (optional, non-negative if present)
 * @param source          origin of the food entry
 */
@Schema(description = "Food catalog entry")
public record FoodDTO(
        @Schema(description = "Food identifier")
        UUID id,

        @NotBlank
        @Schema(description = "Food name", example = "Chicken Breast")
        String name,

        @Schema(description = "Brand name", example = "Freshness Farm")
        String brand,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Kilocalories per 100 g", example = "165.00")
        BigDecimal kcalPer100,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Grams of protein per 100 g", example = "31.00")
        BigDecimal proteinPer100,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Grams of carbohydrates per 100 g", example = "0.00")
        BigDecimal carbsPer100,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Grams of fat per 100 g", example = "3.60")
        BigDecimal fatPer100,

        @PositiveOrZero
        @Schema(description = "Grams of dietary fibre per 100 g (optional)", example = "0.00")
        BigDecimal fiberPer100,

        @PositiveOrZero
        @Schema(description = "Default serving size in grams (optional)", example = "100.00")
        BigDecimal defaultServingG,

        @Schema(description = "How the entry was sourced", example = "MANUAL")
        FoodSource source
) {
}
