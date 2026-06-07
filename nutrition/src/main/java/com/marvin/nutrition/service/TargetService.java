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

    private static final int GOAL_CUT_ADJUSTMENT = -500;
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
     * Computes daily nutrition targets from the given profile and the latest weight entry.
     *
     * @param profile     the user's nutrition profile; must not be null
     * @param latestWeight the most recent weight entry; must not be null
     * @return a {@link TargetsDTO} with all computed targets
     * @throws TargetCalculationException if profile or latestWeight is null
     */
    public TargetsDTO computeTargets(ProfileEntity profile, WeightEntryEntity latestWeight) {
        if (profile == null) {
            throw new TargetCalculationException("No nutrition profile found. Please create a profile first.");
        }
        if (latestWeight == null) {
            throw new TargetCalculationException("No weight entry found. Please log your weight first.");
        }

        final double weightKg = latestWeight.getWeightKg().doubleValue();
        final int bmr = resolveBmr(profile, weightKg);
        final int maintenanceKcal = computeMaintenance(bmr, profile.getActivityLevel());
        final int targetKcal = maintenanceKcal + goalAdjustment(profile.getGoal());

        final int proteinG = (int) Math.round(profile.getProteinPerKg().doubleValue() * weightKg);
        final int fatG = (int) (profile.getFatPct()
                .multiply(BigDecimal.valueOf(targetKcal))
                .divide(BigDecimal.valueOf(KCAL_PER_GRAM_FAT), 4, RoundingMode.HALF_UP)
                .intValue());
        final int proteinKcal = proteinG * KCAL_PER_GRAM_PROTEIN;
        final int fatKcal = fatG * KCAL_PER_GRAM_FAT;
        final int remaining = targetKcal - proteinKcal - fatKcal;
        final int carbsG = remaining / KCAL_PER_GRAM_CARBS;

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
     * @return BMR rounded to the nearest whole kcal
     */
    private int resolveBmr(ProfileEntity profile, double weightKg) {
        if (profile.getBasalKcal() != null) {
            return profile.getBasalKcal();
        }
        return computeMifflinBmr(profile, weightKg);
    }

    /**
     * Computes BMR using the Mifflin–St Jeor formula.
     *
     * @param profile  the user profile supplying sex, birth date and height
     * @param weightKg current body weight in kilograms
     * @return BMR rounded to the nearest whole kcal
     */
    private int computeMifflinBmr(ProfileEntity profile, double weightKg) {
        final int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
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
     * @param goal the dietary goal
     * @return positive, zero, or negative integer kcal adjustment
     */
    private int goalAdjustment(Goal goal) {
        return switch (goal) {
            case CUT -> GOAL_CUT_ADJUSTMENT;
            case MAINTAIN -> GOAL_MAINTAIN_ADJUSTMENT;
            case BULK -> GOAL_BULK_ADJUSTMENT;
        };
    }
}
