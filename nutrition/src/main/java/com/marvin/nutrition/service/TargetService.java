package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.ActivityLevel;
import com.marvin.nutrition.entity.Goal;
import com.marvin.nutrition.entity.ProfileEntity;
import com.marvin.nutrition.entity.Sex;
import com.marvin.nutrition.entity.WeightEntryEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Service;

/**
 * Pure calculation service for daily nutrition targets.
 *
 * <p>All I/O is handled by the orchestrating services; this class only contains
 * deterministic, side-effect-free logic and is therefore easy to unit-test.</p>
 */
@Service
public class TargetService {

    private static final double ACTIVITY_SEDENTARY = 1.2;
    private static final double ACTIVITY_LIGHT = 1.375;
    private static final double ACTIVITY_MODERATE = 1.55;
    private static final double ACTIVITY_ACTIVE = 1.725;
    private static final double ACTIVITY_VERY_ACTIVE = 1.9;

    private static final double CUT_WEEKLY_LOSS_PCT = 0.7;
    private static final int KCAL_PER_KG_FAT = 7700;
    private static final int GOAL_MAINTAIN_ADJUSTMENT = 0;
    private static final int GOAL_BULK_ADJUSTMENT = 300;

    private static final int KCAL_PER_GRAM_PROTEIN = 4;
    private static final int KCAL_PER_GRAM_FAT = 9;
    private static final int KCAL_PER_GRAM_CARBS = 4;

    private static final double MIFFLIN_WEIGHT_FACTOR = 10.0;
    private static final double MIFFLIN_HEIGHT_FACTOR = 6.25;
    private static final double MIFFLIN_AGE_FACTOR = 5.0;
    private static final double MIFFLIN_MALE_CONSTANT = 5.0;
    private static final double MIFFLIN_FEMALE_CONSTANT = -161.0;

    /**
     * Computes daily nutrition targets from the given profile and the latest weight entry,
     * using today's date to determine age.
     *
     * @param profile     the user's nutrition profile; must not be null
     * @param latestWeight the most recent weight entry; must not be null
     * @return a {@link TargetsDTO} with all computed targets
     * @throws TargetCalculationException if profile or latestWeight is null
     */
    public TargetsDTO computeTargets(ProfileEntity profile, WeightEntryEntity latestWeight) {
        return computeTargets(profile, latestWeight, LocalDate.now());
    }

    /**
     * Computes daily nutrition targets from the given profile and weight entry, computing age
     * as of the given date rather than today. This allows historical day summaries to reflect
     * the targets that applied on that day.
     *
     * @param profile      the user's nutrition profile; must not be null
     * @param latestWeight the applicable weight entry; must not be null
     * @param asOfDate     the date to use when computing age
     * @return a {@link TargetsDTO} with all computed targets
     * @throws TargetCalculationException if profile or latestWeight is null
     */
    public TargetsDTO computeTargets(ProfileEntity profile, WeightEntryEntity latestWeight, LocalDate asOfDate) {
        if (profile == null) {
            throw new TargetCalculationException("No nutrition profile found. Please create a profile first.");
        }
        if (latestWeight == null) {
            throw new TargetCalculationException("No weight entry found. Please log your weight first.");
        }

        final double weightKg = latestWeight.getWeightKg().doubleValue();
        final int bmr = resolveBmr(profile, weightKg, asOfDate);
        final int maintenanceKcal = computeMaintenance(bmr, profile.getActivityLevel());
        final int targetKcal = maintenanceKcal + goalAdjustment(profile.getGoal(), weightKg);

        final int proteinG = (int) Math.round(profile.getProteinPerKg().doubleValue() * weightKg);
        final int fatG = profile.getFatPct()
                .multiply(BigDecimal.valueOf(targetKcal))
                .divide(BigDecimal.valueOf(KCAL_PER_GRAM_FAT), 0, RoundingMode.HALF_UP)
                .intValue();
        final int proteinKcal = proteinG * KCAL_PER_GRAM_PROTEIN;
        final int fatKcal = fatG * KCAL_PER_GRAM_FAT;
        final int remaining = Math.max(0, targetKcal - proteinKcal - fatKcal);
        final int carbsG = BigDecimal.valueOf(remaining)
                .divide(BigDecimal.valueOf(KCAL_PER_GRAM_CARBS), 0, RoundingMode.HALF_UP)
                .intValue();

        final String basis = profile.getBasalKcal() != null
                ? TargetBasis.BASAL_KCAL.name()
                : TargetBasis.MIFFLIN_ST_JEOR.name();

        return new TargetsDTO(bmr, maintenanceKcal, targetKcal, proteinG, fatG, carbsG, basis);
    }

    /**
     * Resolves BMR: uses the manual override when set, otherwise computes Mifflin–St Jeor.
     *
     * @param profile  the user profile containing optional {@code basalKcal} override
     * @param weightKg current body weight in kilograms
     * @param asOfDate the date to use when computing age
     * @return BMR rounded to the nearest whole kcal
     */
    private int resolveBmr(ProfileEntity profile, double weightKg, LocalDate asOfDate) {
        if (profile.getBasalKcal() != null) {
            return profile.getBasalKcal();
        }
        return computeMifflinBmr(profile, weightKg, asOfDate);
    }

    /**
     * Computes BMR using the Mifflin–St Jeor formula.
     *
     * @param profile  the user profile supplying sex, birth date and height
     * @param weightKg current body weight in kilograms
     * @param asOfDate the date to use when computing age
     * @return BMR rounded to the nearest whole kcal
     */
    private int computeMifflinBmr(ProfileEntity profile, double weightKg, LocalDate asOfDate) {
        final int age = Period.between(profile.getBirthDate(), asOfDate).getYears();
        final double heightCm = profile.getHeightCm().doubleValue();
        final double sexConstant = profile.getSex() == Sex.MALE
                ? MIFFLIN_MALE_CONSTANT
                : MIFFLIN_FEMALE_CONSTANT;
        final double bmrRaw = MIFFLIN_WEIGHT_FACTOR * weightKg
                + MIFFLIN_HEIGHT_FACTOR * heightCm
                - MIFFLIN_AGE_FACTOR * age
                + sexConstant;
        return (int) Math.round(bmrRaw);
    }

    /**
     * Applies the activity multiplier to BMR to obtain maintenance calories.
     *
     * @param bmr           basal metabolic rate in kcal
     * @param activityLevel the user's physical activity level
     * @return maintenance calories rounded to the nearest whole number
     */
    private int computeMaintenance(int bmr, ActivityLevel activityLevel) {
        final double factor = switch (activityLevel) {
            case SEDENTARY -> ACTIVITY_SEDENTARY;
            case LIGHT -> ACTIVITY_LIGHT;
            case MODERATE -> ACTIVITY_MODERATE;
            case ACTIVE -> ACTIVITY_ACTIVE;
            case VERY_ACTIVE -> ACTIVITY_VERY_ACTIVE;
        };
        return (int) Math.round(bmr * factor);
    }

    /**
     * Returns the kcal adjustment for the given dietary goal.
     *
     * <p>For {@code CUT}, the deficit is scaled to bodyweight rather than a flat value, following
     * Garthe et al. 2011, which found that a weekly bodyweight loss rate of about 0.7% best
     * preserves lean mass during a cut. The weekly deficit is derived from that target loss rate
     * assuming roughly {@value #KCAL_PER_KG_FAT} kcal per kg of fat mass, then spread evenly
     * across the week to obtain the daily adjustment.</p>
     *
     * @param goal     the dietary goal
     * @param weightKg current body weight in kilograms, used to scale the CUT deficit
     * @return positive, zero, or negative integer kcal adjustment
     */
    private int goalAdjustment(Goal goal, double weightKg) {
        return switch (goal) {
            case CUT -> -cutDailyDeficit(weightKg);
            case MAINTAIN -> GOAL_MAINTAIN_ADJUSTMENT;
            case BULK -> GOAL_BULK_ADJUSTMENT;
        };
    }

    /**
     * Computes the daily kcal deficit for a CUT goal, scaled to bodyweight per Garthe et al. 2011.
     *
     * @param weightKg current body weight in kilograms
     * @return the daily kcal deficit, rounded to the nearest whole kcal
     */
    private int cutDailyDeficit(double weightKg) {
        final double weeklyDeficitKcal = weightKg * (CUT_WEEKLY_LOSS_PCT / 100) * KCAL_PER_KG_FAT;
        return (int) Math.round(weeklyDeficitKcal / 7);
    }
}
