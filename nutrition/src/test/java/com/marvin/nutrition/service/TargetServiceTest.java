package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.ActivityLevel;
import com.marvin.nutrition.entity.Goal;
import com.marvin.nutrition.entity.ProfileEntity;
import com.marvin.nutrition.entity.Sex;
import com.marvin.nutrition.entity.WeightEntryEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link TargetService} covering Mifflin–St Jeor, basal_kcal override, activity factors, goals and macros. */
@ExtendWith(MockitoExtension.class)
@DisplayName("TargetService Tests")
class TargetServiceTest {

    private TargetService targetService;

    @BeforeEach
    void setUp() {
        targetService = new TargetService();
    }

    // -----------------------------------------------------------------------
    // Helper builders
    // -----------------------------------------------------------------------

    private ProfileEntity profile(Sex sex, LocalDate birthDate, double heightCm,
            ActivityLevel activityLevel, Goal goal,
            double proteinPerKg, double fatPct, Integer basalKcal) {
        final ProfileEntity p = new ProfileEntity();
        p.setSex(sex);
        p.setBirthDate(birthDate);
        p.setHeightCm(BigDecimal.valueOf(heightCm));
        p.setActivityLevel(activityLevel);
        p.setGoal(goal);
        p.setProteinPerKg(BigDecimal.valueOf(proteinPerKg));
        p.setFatPct(BigDecimal.valueOf(fatPct));
        p.setBasalKcal(basalKcal);
        return p;
    }

    private WeightEntryEntity weight(double kg) {
        final WeightEntryEntity w = new WeightEntryEntity();
        w.setWeightKg(BigDecimal.valueOf(kg));
        return w;
    }

    // -----------------------------------------------------------------------
    // Mifflin–St Jeor: MALE
    // -----------------------------------------------------------------------

    /**
     * Male, 30 years, 175 cm, 80 kg → BMR = 10*80 + 6.25*175 - 5*30 + 5 = 800+1093.75-150+5 = 1748.75 → 1749.
     * MODERATE activity: 1749 * 1.55 = 2710.95 → 2711.
     * MAINTAIN goal: 2711 + 0 = 2711.
     * protein = 2.0 * 80 = 160 g, fat = 0.30 * 2711 / 9 = 813.3 / 9 = 90.37 → 90 g,
     * carbs = (2711 - 160*4 - 90*9) / 4 = (2711 - 640 - 810) / 4 = 1261 / 4 = 315.25 → 315 g.
     */
    @Test
    @DisplayName("MALE MODERATE MAINTAIN uses Mifflin–St Jeor and returns correct macros")
    void compute_MaleModerateMaintain_MifflinFormula() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 2.0, 0.30, null);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertEquals(1749, result.bmr());
        assertEquals(2711, result.maintenanceKcal());
        assertEquals(2711, result.targetKcal());
        assertEquals(160, result.proteinG());
        assertEquals(90, result.fatG());
        assertEquals(315, result.carbsG());
        assertEquals(TargetBasis.MIFFLIN_ST_JEOR.name(), result.basis());
    }

    // -----------------------------------------------------------------------
    // Mifflin–St Jeor: FEMALE
    // -----------------------------------------------------------------------

    /**
     * Female, 25 years, 165 cm, 60 kg → BMR = 10*60 + 6.25*165 - 5*25 - 161 = 600+1031.25-125-161 = 1345.25 → 1345.
     * LIGHT activity: 1345 * 1.375 = 1849.375 → 1849.
     * CUT goal: 1849 - 500 = 1349.
     * protein = 2.0 * 60 = 120 g, fat = round(0.30 * 1349 / 9) = round(44.97) = 45 g,
     * carbs = round((1349 - 120*4 - 45*9) / 4) = round((1349 - 480 - 405) / 4) = round(464 / 4) = round(116) = 116 g.
     */
    @Test
    @DisplayName("FEMALE LIGHT CUT uses Mifflin–St Jeor and returns correct macros")
    void compute_FemaleLightCut_MifflinFormula() {
        final LocalDate birthDate = LocalDate.now().minusYears(25);
        final ProfileEntity p = profile(Sex.FEMALE, birthDate, 165.0,
                ActivityLevel.LIGHT, Goal.CUT, 2.0, 0.30, null);
        final WeightEntryEntity w = weight(60.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertEquals(1345, result.bmr());
        assertEquals(1849, result.maintenanceKcal());
        assertEquals(1349, result.targetKcal());
        assertEquals(120, result.proteinG());
        assertEquals(45, result.fatG());
        assertEquals(116, result.carbsG());
        assertEquals(TargetBasis.MIFFLIN_ST_JEOR.name(), result.basis());
    }

    // -----------------------------------------------------------------------
    // basal_kcal override
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Manual basal_kcal overrides Mifflin–St Jeor formula")
    void compute_BasalKcalOverride_UsesMeasuredBmr() {
        final LocalDate birthDate = LocalDate.now().minusYears(35);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 180.0,
                ActivityLevel.SEDENTARY, Goal.MAINTAIN, 2.0, 0.25, 2000);
        final WeightEntryEntity w = weight(90.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertEquals(2000, result.bmr());
        assertEquals(TargetBasis.BASAL_KCAL.name(), result.basis());
    }

    @Test
    @DisplayName("Clearing basal_kcal (null) falls back to Mifflin–St Jeor")
    void compute_BasalKcalCleared_FallsBackToFormula() {
        final LocalDate birthDate = LocalDate.now().minusYears(35);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 180.0,
                ActivityLevel.SEDENTARY, Goal.MAINTAIN, 2.0, 0.25, null);
        final WeightEntryEntity w = weight(90.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertEquals(TargetBasis.MIFFLIN_ST_JEOR.name(), result.basis());
    }

    // -----------------------------------------------------------------------
    // Activity factors
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("SEDENTARY activity factor 1.2 is applied correctly")
    void compute_Sedentary_AppliesCorrectFactor() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.SEDENTARY, Goal.MAINTAIN, 2.0, 0.30, 1800);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        // maintenance = round(1800 * 1.2) = 2160
        assertEquals(2160, result.maintenanceKcal());
    }

    @Test
    @DisplayName("LIGHT activity factor 1.375 is applied correctly")
    void compute_Light_AppliesCorrectFactor() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.LIGHT, Goal.MAINTAIN, 2.0, 0.30, 1800);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        // maintenance = round(1800 * 1.375) = 2475
        assertEquals(2475, result.maintenanceKcal());
    }

    @Test
    @DisplayName("ACTIVE activity factor 1.725 is applied correctly")
    void compute_Active_AppliesCorrectFactor() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.ACTIVE, Goal.MAINTAIN, 2.0, 0.30, 1800);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        // maintenance = round(1800 * 1.725) = 3105
        assertEquals(3105, result.maintenanceKcal());
    }

    @Test
    @DisplayName("VERY_ACTIVE activity factor 1.9 is applied correctly")
    void compute_VeryActive_AppliesCorrectFactor() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.VERY_ACTIVE, Goal.MAINTAIN, 2.0, 0.30, 1800);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        // maintenance = round(1800 * 1.9) = 3420
        assertEquals(3420, result.maintenanceKcal());
    }

    // -----------------------------------------------------------------------
    // Goal adjustments
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("CUT goal subtracts 500 kcal from maintenance")
    void compute_CutGoal_Subtracts500() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.SEDENTARY, Goal.CUT, 2.0, 0.30, 2000);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        // maintenance = round(2000 * 1.2) = 2400; target = 2400 - 500 = 1900
        assertEquals(2400, result.maintenanceKcal());
        assertEquals(1900, result.targetKcal());
    }

    @Test
    @DisplayName("BULK goal adds 300 kcal to maintenance")
    void compute_BulkGoal_Adds300() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.SEDENTARY, Goal.BULK, 2.0, 0.30, 2000);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        // maintenance = round(2000 * 1.2) = 2400; target = 2400 + 300 = 2700
        assertEquals(2400, result.maintenanceKcal());
        assertEquals(2700, result.targetKcal());
    }

    // -----------------------------------------------------------------------
    // Macro math
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Macro grams are rounded to whole numbers and carbs fill remaining kcal")
    void compute_MacroCarbsRemainder() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.SEDENTARY, Goal.MAINTAIN, 1.8, 0.28, 2000);
        final WeightEntryEntity w = weight(75.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        // targetKcal = round(2000 * 1.2) = 2400
        // protein = round(1.8 * 75) = 135 g → 135 * 4 = 540 kcal
        // fat = round(0.28 * 2400 / 9) = round(74.666...) = 75 g → 75 * 9 = 675 kcal
        // remaining = 2400 - 540 - 675 = 1185 kcal
        // carbs = round(1185 / 4) = round(296.25) = 296 g
        assertEquals(2400, result.targetKcal());
        assertEquals(135, result.proteinG());
        assertEquals(75, result.fatG());
        assertEquals(296, result.carbsG());
    }

    /**
     * Boundary case where the old truncating logic and the new HALF_UP logic disagree.
     *
     * <p>basalKcal = 2000, SEDENTARY → maintenance = round(2000 * 1.2) = 2400, MAINTAIN → targetKcal = 2400.
     * protein = round(2.0 * 70) = 140 g → 140 * 4 = 560 kcal.
     * fat = round(0.246 * 2400 / 9) = round(65.6) = 66 g (old truncating logic gave 65) → 66 * 9 = 594 kcal.
     * remaining = 2400 - 560 - 594 = 1246 kcal.
     * carbs = round(1246 / 4) = round(311.5) = 312 g (old integer division gave 311).</p>
     */
    @Test
    @DisplayName("Fat and carbs are rounded HALF_UP at the .5 boundary instead of truncated")
    void compute_FatAndCarbsAtHalfUpBoundary_RoundUp() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.SEDENTARY, Goal.MAINTAIN, 2.0, 0.246, 2000);
        final WeightEntryEntity w = weight(70.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertEquals(2400, result.targetKcal());
        assertEquals(140, result.proteinG());
        assertEquals(66, result.fatG());
        assertEquals(312, result.carbsG());
    }

    // -----------------------------------------------------------------------
    // Carbs clamp
    // -----------------------------------------------------------------------

    /**
     * Very low weight + aggressive CUT + high protein/fat can push proteinKcal + fatKcal above targetKcal.
     * basalKcal = 1400, weight = 45 kg, SEDENTARY → maintenance = round(1400 * 1.2) = 1680, CUT target = 1180.
     * protein = round(3.5 * 45) = 158 g → 158 * 4 = 632 kcal.
     * fat = round(0.55 * 1180 / 9) = round(72.11) = 72 g → 72 * 9 = 648 kcal.
     * remaining = 1180 - 632 - 648 = -100 kcal → clamped to 0, carbsG must be 0.
     */
    @Test
    @DisplayName("Carbs are clamped to 0 when protein+fat kcal exceeds targetKcal")
    void compute_HighProteinFatExceedsTarget_CarbsClampedToZero() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.FEMALE, birthDate, 160.0,
                ActivityLevel.SEDENTARY, Goal.CUT, 3.5, 0.55, 1400);
        final WeightEntryEntity w = weight(45.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertEquals(0, result.carbsG());
    }

    /**
     * Same scenario as {@link #compute_HighProteinFatExceedsTarget_CarbsClampedToZero()}: protein+fat
     * kcal (1280) exceeds targetKcal (1180), so the macro split is infeasible and must be signalled.
     */
    @Test
    @DisplayName("macrosFeasible is false when protein+fat kcal exceeds targetKcal")
    void compute_HighProteinFatExceedsTarget_MacrosFeasibleIsFalse() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.FEMALE, birthDate, 160.0,
                ActivityLevel.SEDENTARY, Goal.CUT, 3.5, 0.55, 1400);
        final WeightEntryEntity w = weight(45.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertFalse(result.macrosFeasible());
    }

    @Test
    @DisplayName("macrosFeasible is true when protein+fat kcal does not exceed targetKcal")
    void compute_NormalMacros_MacrosFeasibleIsTrue() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 2.0, 0.30, null);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO result = targetService.computeTargets(p, w);

        assertTrue(result.macrosFeasible());
        assertTrue(result.carbsG() > 0);
    }

    // -----------------------------------------------------------------------
    // Error cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Throws TargetCalculationException when profile is null")
    void compute_NullProfile_ThrowsException() {
        final WeightEntryEntity w = weight(80.0);

        assertThrows(TargetCalculationException.class,
                () -> targetService.computeTargets(null, w));
    }

    @Test
    @DisplayName("Throws TargetCalculationException when weight entry is null")
    void compute_NullWeight_ThrowsException() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 2.0, 0.30, null);

        assertThrows(TargetCalculationException.class,
                () -> targetService.computeTargets(p, null));
    }

    // -----------------------------------------------------------------------
    // Date-aware age computation (asOfDate overload)
    // -----------------------------------------------------------------------

    /**
     * A person born on 2000-06-15 is 24 as of 2024-06-14 (birthday not yet reached)
     * but 25 as of 2024-06-15 (birthday reached). With everything else fixed,
     * the BMR (and therefore maintenance/target/carbs) must reflect the age as of
     * the given date, not as of "today".
     */
    @Test
    @DisplayName("computeTargets(profile, weight, asOfDate) computes age as of the given date")
    void compute_WithAsOfDate_UsesAgeAsOfThatDate() {
        final LocalDate birthDate = LocalDate.of(2000, 6, 15);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 2.0, 0.30, null);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO beforeBirthday = targetService.computeTargets(p, w, LocalDate.of(2024, 6, 14));
        final TargetsDTO onBirthday = targetService.computeTargets(p, w, LocalDate.of(2024, 6, 15));

        // BMR = 10*80 + 6.25*175 - 5*age + 5 = 1898.75 - 5*age
        // 2024-06-14 (day before 24th birthday): age 23 -> 1898.75 - 115 = 1783.75 -> 1784
        // 2024-06-15 (24th birthday): age 24 -> 1898.75 - 120 = 1778.75 -> 1779
        assertEquals(1784, beforeBirthday.bmr());
        assertEquals(1779, onBirthday.bmr());
    }

    @Test
    @DisplayName("computeTargets(profile, weight) (2-arg) delegates to asOfDate overload using today")
    void compute_TwoArgOverload_DelegatesUsingToday() {
        final LocalDate birthDate = LocalDate.now().minusYears(30);
        final ProfileEntity p = profile(Sex.MALE, birthDate, 175.0,
                ActivityLevel.MODERATE, Goal.MAINTAIN, 2.0, 0.30, null);
        final WeightEntryEntity w = weight(80.0);

        final TargetsDTO twoArgResult = targetService.computeTargets(p, w);
        final TargetsDTO threeArgResult = targetService.computeTargets(p, w, LocalDate.now());

        assertEquals(threeArgResult, twoArgResult);
    }
}
