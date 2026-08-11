package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.WeightNutrientRatioDTO;
import com.marvin.nutrition.entity.MealEntryEntity;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.repository.MealEntryRepository;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/** Unit tests for {@link WeightNutrientRatioService} covering applicable-weight resolution and ratio computation. */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeightNutrientRatioService Tests")
class WeightNutrientRatioServiceTest {

    @Mock
    private MealEntryRepository mealEntryRepository;

    @Mock
    private WeightEntryRepository weightEntryRepository;

    @InjectMocks
    private WeightNutrientRatioService weightNutrientRatioService;

    private LocalDate day1;
    private LocalDate day2;
    private LocalDate day3;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        day1 = LocalDate.of(2026, 1, 1);
        day2 = LocalDate.of(2026, 1, 2);
        day3 = LocalDate.of(2026, 1, 3);
    }

    private MealEntryEntity mealEntry(
            LocalDate date, String kcal, String protein, String carbs, String fat) {
        final MealEntryEntity entity = new MealEntryEntity();
        entity.setEntryDate(date);
        entity.setMealType(MealType.LUNCH);
        entity.setKcal(new BigDecimal(kcal));
        entity.setProteinG(new BigDecimal(protein));
        entity.setCarbsG(new BigDecimal(carbs));
        entity.setFatG(new BigDecimal(fat));
        return entity;
    }

    private WeightEntryEntity weightEntry(LocalDate date, String weightKg) {
        final WeightEntryEntity entity = new WeightEntryEntity();
        entity.setEntryDate(date);
        entity.setWeightKg(new BigDecimal(weightKg));
        return entity;
    }

    // -----------------------------------------------------------------------
    // continuous weight history / day without meal entries
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getRatios computes per-day ratios against continuous weight history, "
            + "and yields zero ratios for a day with no meal entries")
    void getRatios_ContinuousWeightHistory_ComputesRatiosAndZeroForEmptyDay() {
        final WeightEntryEntity weight = weightEntry(LocalDate.of(2025, 12, 31), "80.00");

        when(mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(day1, day3))
                .thenReturn(List.of(
                        mealEntry(day1, "2000.00", "160.00", "200.00", "60.00"),
                        mealEntry(day3, "1600.00", "80.00", "100.00", "40.00")
                ));
        when(weightEntryRepository.findByEntryDateLessThanEqualOrderByEntryDateAsc(day3))
                .thenReturn(List.of(weight));
        when(weightEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.of(weight));

        StepVerifier.create(weightNutrientRatioService.getRatios(day1, day3))
                .assertNext(results -> {
                    assertEquals(3, results.size());

                    final WeightNutrientRatioDTO d1 = results.get(0);
                    assertEquals(day1, d1.date());
                    assertEquals(new BigDecimal("80.00"), d1.weightKg());
                    assertEquals(new BigDecimal("25.00"), d1.kcalPerKg());
                    assertEquals(new BigDecimal("2.00"), d1.proteinPerKg());
                    assertEquals(new BigDecimal("2.50"), d1.carbsPerKg());
                    assertEquals(new BigDecimal("0.75"), d1.fatPerKg());

                    final WeightNutrientRatioDTO d2 = results.get(1);
                    assertEquals(day2, d2.date());
                    assertEquals(new BigDecimal("80.00"), d2.weightKg());
                    assertEquals(BigDecimal.ZERO.setScale(2), d2.kcal());
                    assertEquals(new BigDecimal("0.00"), d2.kcalPerKg());
                    assertEquals(new BigDecimal("0.00"), d2.proteinPerKg());
                    assertEquals(new BigDecimal("0.00"), d2.carbsPerKg());
                    assertEquals(new BigDecimal("0.00"), d2.fatPerKg());

                    final WeightNutrientRatioDTO d3 = results.get(2);
                    assertEquals(day3, d3.date());
                    assertEquals(new BigDecimal("80.00"), d3.weightKg());
                    assertEquals(new BigDecimal("20.00"), d3.kcalPerKg());
                    assertEquals(new BigDecimal("1.00"), d3.proteinPerKg());
                    assertEquals(new BigDecimal("1.25"), d3.carbsPerKg());
                    assertEquals(new BigDecimal("0.50"), d3.fatPerKg());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // range entirely before the first-ever weight entry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getRatios falls back to the earliest-ever weight entry (not the latest) "
            + "for days before the first-ever weight entry")
    void getRatios_DaysBeforeFirstWeightEntry_FallsBackToEarliestNotLatest() {
        final WeightEntryEntity earliest = weightEntry(LocalDate.of(2026, 1, 10), "75.00");
        final WeightEntryEntity latest = weightEntry(LocalDate.of(2026, 1, 20), "90.00");

        when(mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(day1, day3))
                .thenReturn(List.of(mealEntry(day1, "1500.00", "100.00", "150.00", "50.00")));
        when(weightEntryRepository.findByEntryDateLessThanEqualOrderByEntryDateAsc(day3))
                .thenReturn(List.of());
        when(weightEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.of(earliest));

        StepVerifier.create(weightNutrientRatioService.getRatios(day1, day3))
                .assertNext(results -> {
                    for (final WeightNutrientRatioDTO dto : results) {
                        assertEquals(new BigDecimal("75.00"), dto.weightKg());
                        assertNotEquals(latest.getWeightKg(), dto.weightKg());
                    }
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // weight history starting partway through the range
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getRatios uses the earliest-ever fallback before the first entry in range, "
            + "then switches to each subsequent entry as it becomes applicable")
    void getRatios_WeightHistoryStartsLaterInRange_SwitchesFromFallbackToActualEntries() {
        final LocalDate from = LocalDate.of(2026, 1, 1);
        final LocalDate to = LocalDate.of(2026, 1, 5);
        final WeightEntryEntity firstEntry = weightEntry(LocalDate.of(2026, 1, 3), "80.00");
        final WeightEntryEntity secondEntry = weightEntry(LocalDate.of(2026, 1, 4), "82.00");

        when(mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to))
                .thenReturn(List.of());
        when(weightEntryRepository.findByEntryDateLessThanEqualOrderByEntryDateAsc(to))
                .thenReturn(List.of(firstEntry, secondEntry));
        when(weightEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.of(firstEntry));

        StepVerifier.create(weightNutrientRatioService.getRatios(from, to))
                .assertNext(results -> {
                    assertEquals(5, results.size());
                    assertEquals(new BigDecimal("80.00"), results.get(0).weightKg());
                    assertEquals(new BigDecimal("80.00"), results.get(1).weightKg());
                    assertEquals(new BigDecimal("80.00"), results.get(2).weightKg());
                    assertEquals(new BigDecimal("82.00"), results.get(3).weightKg());
                    assertEquals(new BigDecimal("82.00"), results.get(4).weightKg());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // totally empty weight history
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getRatios yields null weightKg and null ratios when no weight entries exist at all")
    void getRatios_EmptyWeightHistory_WeightAndRatiosAreNull() {
        when(mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(day1, day3))
                .thenReturn(List.of(mealEntry(day1, "2000.00", "160.00", "200.00", "60.00")));
        when(weightEntryRepository.findByEntryDateLessThanEqualOrderByEntryDateAsc(day3))
                .thenReturn(List.of());
        when(weightEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.empty());

        StepVerifier.create(weightNutrientRatioService.getRatios(day1, day3))
                .assertNext(results -> {
                    assertEquals(3, results.size());
                    for (final WeightNutrientRatioDTO dto : results) {
                        assertNull(dto.weightKg());
                        assertNull(dto.kcalPerKg());
                        assertNull(dto.proteinPerKg());
                        assertNull(dto.carbsPerKg());
                        assertNull(dto.fatPerKg());
                    }
                })
                .verifyComplete();
    }
}
