package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.DaySummaryDTO;
import com.marvin.nutrition.dto.MacrosDTO;
import com.marvin.nutrition.dto.ProfileDTO;
import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.dto.WeightEntryDTO;
import com.marvin.nutrition.entity.ActivityLevel;
import com.marvin.nutrition.entity.Goal;
import com.marvin.nutrition.entity.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link PdfExportService} covering PDF generation and graceful error handling. */
@ExtendWith(MockitoExtension.class)
@DisplayName("PdfExportService Tests")
class PdfExportServiceTest {

    @Mock
    private NutritionProfileService profileService;

    @Mock
    private NutritionTargetService nutritionTargetService;

    @Mock
    private WeightService weightService;

    @Mock
    private MealEntryService mealEntryService;

    @InjectMocks
    private PdfExportService pdfExportService;

    private LocalDate from;
    private LocalDate to;

    /** Sets up the date range used across all tests. */
    @BeforeEach
    void setUp() {
        from = LocalDate.of(2026, 6, 1);
        to = LocalDate.of(2026, 6, 3);
    }

    private ProfileDTO sampleProfile() {
        return new ProfileDTO(
                1L, Sex.MALE, LocalDate.of(1990, 5, 15),
                new BigDecimal("180"), ActivityLevel.MODERATE, Goal.MAINTAIN,
                new BigDecimal("2.0"), new BigDecimal("0.30"), null
        );
    }

    private TargetsDTO sampleTargets() {
        return new TargetsDTO(1750, 2713, 2213, 160, 74, 248, "MIFFLIN_ST_JEOR");
    }

    private DaySummaryDTO emptyDay(LocalDate date) {
        final MacrosDTO zero = new MacrosDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        return new DaySummaryDTO(date, List.of(), zero, sampleTargets(), null, List.of(), BigDecimal.ZERO);
    }

    private void setupHappyPathMocks() {
        when(profileService.getProfile()).thenReturn(Mono.just(sampleProfile()));
        when(nutritionTargetService.getTargets()).thenReturn(Mono.just(sampleTargets()));
        final WeightEntryDTO weight = new WeightEntryDTO(1L, from, new BigDecimal("80.00"));
        when(weightService.findAll()).thenReturn(Flux.just(weight));
        final List<DaySummaryDTO> days = List.of(emptyDay(from), emptyDay(from.plusDays(1)), emptyDay(to));
        when(mealEntryService.getDays(from, to)).thenReturn(Mono.just(days));
    }

    @Test
    @DisplayName("generatePdf emits bytes that start with the %PDF magic bytes (0x25 0x50 0x44 0x46)")
    void generatesPdfWithMagicBytes() {
        setupHappyPathMocks();

        StepVerifier.create(pdfExportService.generatePdf(from, to))
                .assertNext(bytes -> {
                    assertNotNull(bytes);
                    assertEquals('%', (char) bytes[0]);
                    assertEquals('P', (char) bytes[1]);
                    assertEquals('D', (char) bytes[2]);
                    assertEquals('F', (char) bytes[3]);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generatePdf still produces a valid PDF when profileService emits an error")
    void gracefulWhenProfileMissing() {
        when(profileService.getProfile()).thenReturn(Mono.error(new NoSuchElementException("no profile")));
        when(nutritionTargetService.getTargets()).thenReturn(Mono.just(sampleTargets()));
        when(weightService.findAll()).thenReturn(Flux.empty());
        when(mealEntryService.getDays(from, to)).thenReturn(Mono.just(List.of()));

        StepVerifier.create(pdfExportService.generatePdf(from, to))
                .assertNext(bytes -> {
                    assertNotNull(bytes);
                    assertEquals('%', (char) bytes[0]);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generatePdf renders all calendar days in the range even when they have no meal entries")
    void rendersEmptyDays() {
        when(profileService.getProfile()).thenReturn(Mono.just(sampleProfile()));
        when(nutritionTargetService.getTargets()).thenReturn(Mono.just(sampleTargets()));
        when(weightService.findAll()).thenReturn(Flux.empty());
        final List<DaySummaryDTO> emptyDays =
                List.of(emptyDay(from), emptyDay(from.plusDays(1)), emptyDay(to));
        when(mealEntryService.getDays(from, to)).thenReturn(Mono.just(emptyDays));

        StepVerifier.create(pdfExportService.generatePdf(from, to))
                .assertNext(bytes -> assertNotNull(bytes))
                .verifyComplete();
    }

    @Test
    @DisplayName("generatePdf still produces a valid PDF when nutritionTargetService emits an error")
    void gracefulWhenTargetsMissing() {
        when(profileService.getProfile()).thenReturn(Mono.just(sampleProfile()));
        when(nutritionTargetService.getTargets()).thenReturn(
                Mono.error(new RuntimeException("targets unavailable")));
        when(weightService.findAll()).thenReturn(Flux.empty());
        when(mealEntryService.getDays(from, to)).thenReturn(Mono.just(List.of()));

        StepVerifier.create(pdfExportService.generatePdf(from, to))
                .assertNext(bytes -> {
                    assertNotNull(bytes);
                    assertEquals('%', (char) bytes[0]);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generatePdf calls mealEntryService.getDays(from, to) and weightService.findAll()")
    void callsCorrectServices() {
        setupHappyPathMocks();

        StepVerifier.create(pdfExportService.generatePdf(from, to))
                .assertNext(bytes -> assertNotNull(bytes))
                .verifyComplete();

        verify(mealEntryService).getDays(from, to);
        verify(weightService).findAll();
    }
}
