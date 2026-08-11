package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.WeightNutrientRatioDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioSummaryDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioSummaryResponse;
import com.marvin.nutrition.repository.MealEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Composes {@link WeightNutrientRatioService} into period-level averages: the already-correct
 * per-day macro-to-body-weight ratios are averaged over the "tracked" days (days with logged
 * meals) of the last 30 days and of the entire tracked period, analogous to the averaging pattern
 * in {@code PdfExportService.renderPeriodAverages()}.
 */
@Service
public class WeightNutrientRatioSummaryService {

    private static final int LAST_30_DAYS_WINDOW = 30;

    /** Rounding scale for averaged ratios, matching {@code WeightNutrientRatioService.RATIO_SCALE}. */
    private static final int AVERAGE_SCALE = 2;

    private final WeightNutrientRatioService weightNutrientRatioService;
    private final MealEntryRepository mealEntryRepository;

    /**
     * Creates a new WeightNutrientRatioSummaryService.
     *
     * @param weightNutrientRatioService the service computing per-day weight/nutrient ratios
     * @param mealEntryRepository        JPA repository for meal entries, used to find the earliest one
     */
    public WeightNutrientRatioSummaryService(
            WeightNutrientRatioService weightNutrientRatioService, MealEntryRepository mealEntryRepository) {
        this.weightNutrientRatioService = weightNutrientRatioService;
        this.mealEntryRepository = mealEntryRepository;
    }

    /**
     * Returns the averaged macro-to-body-weight ratios for the last 30 days (inclusive of today)
     * and for the entire tracked period (from the earliest ever recorded meal entry to today).
     *
     * @return a Mono emitting both period summaries
     */
    public Mono<WeightNutrientRatioSummaryResponse> getSummary() {
        final LocalDate today = LocalDate.now();
        final LocalDate last30From = today.minusDays(LAST_30_DAYS_WINDOW - 1);

        final Mono<WeightNutrientRatioSummaryDTO> last30Mono = buildSummary(last30From, today);
        final Mono<WeightNutrientRatioSummaryDTO> totalPeriodMono = Mono.fromCallable(
                        mealEntryRepository::findTopByOrderByEntryDateAsc)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(earliest -> earliest
                        .map(entity -> buildSummary(entity.getEntryDate(), today))
                        .orElseGet(() -> Mono.just(emptySummary())));

        return Mono.zip(last30Mono, totalPeriodMono)
                .map(tuple -> new WeightNutrientRatioSummaryResponse(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Builds a single period summary by fetching the daily ratios for the range and averaging the
     * tracked days.
     *
     * @param from the first date of the period (inclusive)
     * @param to   the last date of the period (inclusive)
     * @return a Mono emitting the assembled summary
     */
    private Mono<WeightNutrientRatioSummaryDTO> buildSummary(LocalDate from, LocalDate to) {
        return weightNutrientRatioService.getRatios(from, to)
                .map(ratios -> {
                    final List<WeightNutrientRatioDTO> trackedDays = ratios.stream()
                            .filter(r -> r.kcal() != null && r.kcal().compareTo(BigDecimal.ZERO) > 0)
                            .toList();
                    final int totalDays = (int) ChronoUnit.DAYS.between(from, to) + 1;
                    return new WeightNutrientRatioSummaryDTO(
                            from,
                            to,
                            totalDays,
                            trackedDays.size(),
                            average(trackedDays, WeightNutrientRatioDTO::proteinPerKg),
                            average(trackedDays, WeightNutrientRatioDTO::carbsPerKg),
                            average(trackedDays, WeightNutrientRatioDTO::fatPerKg));
                });
    }

    /**
     * Averages a per-day ratio value across the tracked days, skipping days where the value is
     * unavailable (no applicable weight entry).
     *
     * @param trackedDays the days to average over
     * @param extractor   extracts the macro-per-kg value from a single day's ratio entry
     * @return the average rounded to {@value #AVERAGE_SCALE} decimal places, or null if no day has a value
     */
    private BigDecimal average(
            List<WeightNutrientRatioDTO> trackedDays, Function<WeightNutrientRatioDTO, BigDecimal> extractor) {
        final List<BigDecimal> values = trackedDays.stream().map(extractor).filter(Objects::nonNull).toList();
        if (values.isEmpty()) {
            return null;
        }
        final BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Builds an empty summary for when no meal entry has ever been recorded, avoiding any
     * division and leaving all fields null or zero as appropriate.
     *
     * @return the empty summary
     */
    private WeightNutrientRatioSummaryDTO emptySummary() {
        return new WeightNutrientRatioSummaryDTO(null, null, 0, 0, null, null, null);
    }
}
