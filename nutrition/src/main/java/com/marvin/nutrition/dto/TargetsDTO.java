package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for computed daily nutrition targets.
 *
 * @param bmr             basal metabolic rate in kcal
 * @param maintenanceKcal maintenance calories (BMR × activity factor)
 * @param targetKcal      daily target calories after goal adjustment
 * @param proteinG        daily protein target in grams
 * @param fatG            daily fat target in grams
 * @param carbsG          daily carbohydrate target in grams
 * @param basis           how BMR was derived: BASAL_KCAL or MIFFLIN_ST_JEOR
 */
@Schema(description = "Computed daily nutrition targets")
public record TargetsDTO(
        @Schema(description = "Basal metabolic rate in kcal", example = "1750")
        int bmr,

        @Schema(description = "Maintenance calories (BMR × activity factor)", example = "2713")
        int maintenanceKcal,

        @Schema(description = "Daily target calories after goal adjustment", example = "2213")
        int targetKcal,

        @Schema(description = "Daily protein target in grams", example = "160")
        int proteinG,

        @Schema(description = "Daily fat target in grams", example = "74")
        int fatG,

        @Schema(description = "Daily carbohydrate target in grams", example = "248")
        int carbsG,

        @Schema(description = "How BMR was derived", example = "MIFFLIN_ST_JEOR")
        String basis
) {
}
