package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.WeightNutrientRatioDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioSummaryDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioSummaryResponse;
import com.marvin.nutrition.service.WeightNutrientRatioService;
import com.marvin.nutrition.service.WeightNutrientRatioSummaryService;
import com.marvin.nutrition.service.WeightService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link WeightController}, covering the weight/nutrient ratio endpoints. */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeightController Tests")
class WeightControllerTest {

    @Mock
    private WeightService weightService;

    @Mock
    private WeightNutrientRatioService weightNutrientRatioService;

    @Mock
    private WeightNutrientRatioSummaryService weightNutrientRatioSummaryService;

    @InjectMocks
    private WeightController weightController;

    private LocalDate from;
    private LocalDate to;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        from = LocalDate.of(2026, 6, 1);
        to = LocalDate.of(2026, 6, 2);
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/weight/ratios
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getRatios returns 200 with one ratio entry per day in range")
    void getRatios_ReturnsRatios_Returns200WithList() {
        final WeightNutrientRatioDTO day1 = new WeightNutrientRatioDTO(
                from, new BigDecimal("80.00"),
                new BigDecimal("2000.00"), new BigDecimal("160.00"), new BigDecimal("200.00"), new BigDecimal("60.00"),
                new BigDecimal("25.00"), new BigDecimal("2.00"), new BigDecimal("2.50"), new BigDecimal("0.75")
        );
        final WeightNutrientRatioDTO day2 = new WeightNutrientRatioDTO(
                to, new BigDecimal("80.00"),
                new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")
        );

        when(weightNutrientRatioService.getRatios(from, to)).thenReturn(Mono.just(List.of(day1, day2)));

        final Mono<List<WeightNutrientRatioDTO>> result = weightController.getRatios(from, to);

        StepVerifier.create(result)
                .assertNext(results -> {
                    assertEquals(2, results.size());
                    assertEquals(from, results.get(0).date());
                    assertEquals(to, results.get(1).date());
                })
                .verifyComplete();

        verify(weightNutrientRatioService).getRatios(from, to);
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/weight/ratios/summary
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getRatioSummary returns 200 with the last-30-days and total-period summaries")
    void getRatioSummary_ReturnsSummary_Returns200WithBothPeriods() {
        final WeightNutrientRatioSummaryDTO last30Days = new WeightNutrientRatioSummaryDTO(
                from, to, 2, 2, new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("1.00"));
        final WeightNutrientRatioSummaryDTO totalPeriod = new WeightNutrientRatioSummaryDTO(
                from, to, 2, 2, new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("1.00"));
        final WeightNutrientRatioSummaryResponse response =
                new WeightNutrientRatioSummaryResponse(last30Days, totalPeriod);

        when(weightNutrientRatioSummaryService.getSummary()).thenReturn(Mono.just(response));

        final Mono<WeightNutrientRatioSummaryResponse> result = weightController.getRatioSummary();

        StepVerifier.create(result)
                .assertNext(actual -> {
                    assertEquals(last30Days, actual.last30Days());
                    assertEquals(totalPeriod, actual.totalPeriod());
                })
                .verifyComplete();

        verify(weightNutrientRatioSummaryService).getSummary();
    }
}
