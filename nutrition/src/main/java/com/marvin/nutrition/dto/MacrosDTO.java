package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing macro-nutrient totals or remaining values.
 *
 * @param kcal     kilocalories
 * @param proteinG grams of protein
 * @param carbsG   grams of carbohydrates
 * @param fatG     grams of fat
 */
@Schema(description = "Macro-nutrient totals or remaining values for a day")
public record MacrosDTO(
        @Schema(description = "Kilocalories", example = "600.00")
        BigDecimal kcal,

        @Schema(description = "Grams of protein", example = "40.00")
        BigDecimal proteinG,

        @Schema(description = "Grams of carbohydrates", example = "75.00")
        BigDecimal carbsG,

        @Schema(description = "Grams of fat", example = "15.00")
        BigDecimal fatG
) {
}
