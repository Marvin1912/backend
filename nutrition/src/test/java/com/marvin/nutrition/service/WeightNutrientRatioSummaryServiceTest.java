package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.WeightNutrientRatioDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioSummaryDTO;
import com.marvin.nutrition.entity.MealEntryEntity;
import com.marvin.nutrition.repository.MealEntryRepository;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link WeightNutrientRatioSummaryService} covering the averaging of daily ratios
 * over tracked days for the last-30-days and total-period summaries.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeightNutrientRatioSummaryService Tests")
class WeightNutrientRatioSummaryServiceTest {

    @Mock
    private WeightNutrientRatioService weightNutrientRatioService;

    @Mock
    private MealEntryRepository mealEntryRepository;

    @InjectMocks
    private WeightNutrientRatioSummaryService weightNutrientRatioSummaryService;

    private LocalDate today;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    private WeightNutrientRatioDTO ratio(
            LocalDate date, String kcal, BigDecimal proteinPerKg, BigDecimal carbsPerKg, BigDecimal fatPerKg) {
        return new WeightNutrientRatioDTO(
                date, new BigDecimal("80.00"), new BigDecimal(kcal), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1.00"), proteinPerKg, carbsPerKg, fatPerKg);
    }

    private MealEntryEntity mealEntry(LocalDate date) {
        final MealEntryEntity entity = new MealEntryEntity();
        entity.setEntryDate(date);
        return entity;
    }

    // -----------------------------------------------------------------------
    // no meal entries ever recorded
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getSummary returns an all-null total-period summary when no meal entries exist at all")
    void getSummary_NoMealEntriesEver_TotalPeriodIsAllNull() {
        when(weightNutrientRatioService.getRatios(any(), any())).thenReturn(Mono.just(List.of()));
        when(mealEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.empty());

        StepVerifier.create(weightNutrientRatioSummaryService.getSummary())
                .assertNext(response -> {
                    final WeightNutrientRatioSummaryDTO totalPeriod = response.totalPeriod();
                    assertNull(totalPeriod.from());
                    assertNull(totalPeriod.to());
                    assertEquals(0, totalPeriod.totalDays());
                    assertEquals(0, totalPeriod.trackedDays());
                    assertNull(totalPeriod.avgProteinPerKg());
                    assertNull(totalPeriod.avgCarbsPerKg());
                    assertNull(totalPeriod.avgFatPerKg());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // averages only over tracked (kcal > 0) days, skipping untracked ones
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getSummary averages ratios only over days with kcal > 0, ignoring untracked days")
    void getSummary_MixOfTrackedAndUntrackedDays_AveragesOnlyTrackedDays() {
        final LocalDate day1 = today.minusDays(2);
        final LocalDate day2 = today.minusDays(1);
        final LocalDate day3 = today;
        final List<WeightNutrientRatioDTO> ratios = List.of(
                ratio(day1, "2000.00", new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("1.00")),
                ratio(day2, "0.00", null, null, null),
                ratio(day3, "1800.00", new BigDecimal("4.00"), new BigDecimal("5.00"), new BigDecimal("3.00"))
        );
        when(weightNutrientRatioService.getRatios(any(), any())).thenReturn(Mono.just(ratios));
        when(mealEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.of(mealEntry(day1)));

        StepVerifier.create(weightNutrientRatioSummaryService.getSummary())
                .assertNext(response -> {
                    final WeightNutrientRatioSummaryDTO totalPeriod = response.totalPeriod();
                    assertEquals(2, totalPeriod.trackedDays());
                    assertEquals(new BigDecimal("3.00"), totalPeriod.avgProteinPerKg());
                    assertEquals(new BigDecimal("4.00"), totalPeriod.avgCarbsPerKg());
                    assertEquals(new BigDecimal("2.00"), totalPeriod.avgFatPerKg());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // weight data entirely missing -> null averages despite tracked days existing
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getSummary returns null averages when weight data is missing even though days are tracked")
    void getSummary_TrackedDaysButNoWeightData_AveragesAreNull() {
        final LocalDate day1 = today.minusDays(1);
        final LocalDate day2 = today;
        final List<WeightNutrientRatioDTO> ratios = List.of(
                ratio(day1, "2000.00", null, null, null),
                ratio(day2, "1800.00", null, null, null)
        );
        when(weightNutrientRatioService.getRatios(any(), any())).thenReturn(Mono.just(ratios));
        when(mealEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.of(mealEntry(day1)));

        StepVerifier.create(weightNutrientRatioSummaryService.getSummary())
                .assertNext(response -> {
                    final WeightNutrientRatioSummaryDTO totalPeriod = response.totalPeriod();
                    assertEquals(2, totalPeriod.trackedDays());
                    assertNull(totalPeriod.avgProteinPerKg());
                    assertNull(totalPeriod.avgCarbsPerKg());
                    assertNull(totalPeriod.avgFatPerKg());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // no tracked days at all in the period -> null averages, zero tracked days
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getSummary returns null averages and zero tracked days when no day in the period has kcal > 0")
    void getSummary_NoTrackedDaysInPeriod_AveragesAreNullAndTrackedDaysZero() {
        final LocalDate day1 = today.minusDays(1);
        final LocalDate day2 = today;
        final List<WeightNutrientRatioDTO> ratios = List.of(
                ratio(day1, "0.00", new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("1.00")),
                ratio(day2, "0.00", new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("1.00"))
        );
        when(weightNutrientRatioService.getRatios(any(), any())).thenReturn(Mono.just(ratios));
        when(mealEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.of(mealEntry(day1)));

        StepVerifier.create(weightNutrientRatioSummaryService.getSummary())
                .assertNext(response -> {
                    final WeightNutrientRatioSummaryDTO totalPeriod = response.totalPeriod();
                    assertEquals(0, totalPeriod.trackedDays());
                    assertNull(totalPeriod.avgProteinPerKg());
                    assertNull(totalPeriod.avgCarbsPerKg());
                    assertNull(totalPeriod.avgFatPerKg());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // last30Days always spans exactly 30 calendar days ending today
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getSummary computes last30Days as a 30-day window ending today, inclusive")
    void getSummary_Last30Days_SpansThirtyDaysEndingToday() {
        when(weightNutrientRatioService.getRatios(any(), any())).thenReturn(Mono.just(List.of()));
        when(mealEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.empty());

        StepVerifier.create(weightNutrientRatioSummaryService.getSummary())
                .assertNext(response -> {
                    final WeightNutrientRatioSummaryDTO last30Days = response.last30Days();
                    assertEquals(today.minusDays(29), last30Days.from());
                    assertEquals(today, last30Days.to());
                    assertEquals(30, last30Days.totalDays());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // totalPeriod spans from the earliest ever meal entry to today
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getSummary computes totalPeriod from the earliest ever recorded meal entry to today")
    void getSummary_TotalPeriod_SpansFromEarliestMealEntryToToday() {
        final LocalDate earliest = today.minusDays(100);
        when(weightNutrientRatioService.getRatios(any(), any())).thenReturn(Mono.just(List.of()));
        when(mealEntryRepository.findTopByOrderByEntryDateAsc()).thenReturn(Optional.of(mealEntry(earliest)));

        StepVerifier.create(weightNutrientRatioSummaryService.getSummary())
                .assertNext(response -> {
                    final WeightNutrientRatioSummaryDTO totalPeriod = response.totalPeriod();
                    assertEquals(earliest, totalPeriod.from());
                    assertEquals(today, totalPeriod.to());
                    assertEquals(101, totalPeriod.totalDays());
                })
                .verifyComplete();
    }
}
