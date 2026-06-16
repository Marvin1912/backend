package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body for creating a synthetic food entry and meal template from a canteen AI estimate.
 * All macro values represent the total meal (not per-100 g), and are stored as-is since
 * the synthetic food entry uses a fixed 100 g default serving.
 *
 * @param name      the display name of the resulting meal template
 * @param kcal      estimated total kilocalories
 * @param proteinG  estimated total protein in grams
 * @param carbsG    estimated total carbohydrates in grams
 * @param fatG      estimated total fat in grams
 */
@Schema(description = "Request to save a canteen AI estimate as a reusable meal template")
public record SaveEstimateAsTemplateRequest(
        @NotBlank
        @Size(max = 255)
        @Schema(description = "Display name of the resulting meal template", example = "Canteen Lunch")
        String name,

        @NotNull
        @DecimalMin("0")
        @Schema(description = "Estimated total kilocalories", example = "650")
        BigDecimal kcal,

        @NotNull
        @DecimalMin("0")
        @Schema(description = "Estimated total protein in grams", example = "35")
        BigDecimal proteinG,

        @NotNull
        @DecimalMin("0")
        @Schema(description = "Estimated total carbohydrates in grams", example = "70")
        BigDecimal carbsG,

        @NotNull
        @DecimalMin("0")
        @Schema(description = "Estimated total fat in grams", example = "20")
        BigDecimal fatG
) {
}
