package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.ActivityLevel;
import com.marvin.nutrition.entity.Goal;
import com.marvin.nutrition.entity.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for the user's nutrition profile.
 *
 * @param id             surrogate identifier
 * @param sex            biological sex (MALE or FEMALE)
 * @param birthDate      date of birth used to compute age
 * @param heightCm       height in centimetres
 * @param activityLevel  physical activity level
 * @param goal           dietary goal (CUT, MAINTAIN, or BULK)
 * @param proteinPerKg   grams of protein per kilogram of body weight
 * @param fatPct         fraction of target calories to allocate to fat (0–1)
 * @param basalKcal      optional manual override for BMR in kcal; null uses Mifflin–St Jeor
 */
@Schema(description = "User nutrition profile")
public record ProfileDTO(
        @Schema(description = "Profile identifier")
        Long id,

        @NotNull
        @Schema(description = "Biological sex", example = "MALE")
        Sex sex,

        @NotNull
        @Schema(description = "Date of birth", example = "1990-05-15")
        LocalDate birthDate,

        @NotNull
        @Positive
        @Schema(description = "Height in centimetres", example = "175")
        BigDecimal heightCm,

        @NotNull
        @Schema(description = "Physical activity level", example = "MODERATE")
        ActivityLevel activityLevel,

        @NotNull
        @Schema(description = "Dietary goal", example = "MAINTAIN")
        Goal goal,

        @Positive
        @Schema(description = "Grams of protein per kg body weight", example = "2.0")
        BigDecimal proteinPerKg,

        @NotNull
        @Positive
        @DecimalMax("1.0")
        @Schema(description = "Fraction of target calories allocated to fat (0–1)", example = "0.30")
        BigDecimal fatPct,

        @Positive
        @Schema(description = "Manual BMR override in kcal; null uses Mifflin–St Jeor formula", example = "1800")
        Integer basalKcal
) {
}
