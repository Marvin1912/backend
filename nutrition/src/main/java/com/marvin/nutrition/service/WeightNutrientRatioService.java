package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.MacrosDTO;
import com.marvin.nutrition.dto.WeightNutrientRatioDTO;
import com.marvin.nutrition.entity.MealEntryEntity;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.repository.MealEntryRepository;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Computes, for a date range, each day's total nutrient intake paired with the applicable
 * historical body weight and the resulting per-kilogram ratios.
 */
@Service
public class WeightNutrientRatioService {

    private static final int RATIO_SCALE = 2;

    private final MealEntryRepository mealEntryRepository;
    private final WeightEntryRepository weightEntryRepository;

    /**
     * Creates a new WeightNutrientRatioService.
     *
     * @param mealEntryRepository   JPA repository for meal entries
     * @param weightEntryRepository JPA repository for weight entries
     */
    public WeightNutrientRatioService(
            MealEntryRepository mealEntryRepository, WeightEntryRepository weightEntryRepository) {
        this.mealEntryRepository = mealEntryRepository;
        this.weightEntryRepository = weightEntryRepository;
    }

    /**
     * Returns one weight/nutrient-ratio entry per date within the given range (inclusive), in
     * ascending date order.
     *
     * <p>The applicable weight for a given date is the most recent weight entry on or before that
     * date. If no such entry exists (the date is before the very first ever weight entry), the
     * earliest ever weight entry is used as a fallback. Only if there are no weight entries at all
     * is {@code weightKg} (and thus all ratios) {@code null}.</p>
     *
     * @param from the first date to include (inclusive)
     * @param to   the last date to include (inclusive)
     * @return a Mono emitting one ratio entry per date in the range, ordered ascending by date
     */
    public Mono<List<WeightNutrientRatioDTO>> getRatios(LocalDate from, LocalDate to) {
        final Mono<Map<LocalDate, MacrosDTO>> totalsByDateMono = Mono.fromCallable(() -> {
            final List<MealEntryEntity> entries =
                    mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to);
            return computeTotalsByDate(entries);
        }).subscribeOn(Schedulers.boundedElastic());

        final Mono<List<WeightEntryEntity>> weightsUpToMono = Mono.fromCallable(() ->
                weightEntryRepository.findByEntryDateLessThanEqualOrderByEntryDateAsc(to)
        ).subscribeOn(Schedulers.boundedElastic());

        final Mono<Optional<WeightEntryEntity>> earliestEverMono = Mono.fromCallable(() ->
                weightEntryRepository.findTopByOrderByEntryDateAsc()
        ).subscribeOn(Schedulers.boundedElastic());

        return Mono.zip(totalsByDateMono, weightsUpToMono, earliestEverMono)
                .map(tuple -> buildRatios(from, to, tuple.getT1(), tuple.getT2(), tuple.getT3().orElse(null)));
    }

    /**
     * Builds one ratio DTO per date in the range by walking the range and the sorted weight list
     * in lockstep, resolving the applicable weight with a forward pointer.
     *
     * @param from         the first date to include (inclusive)
     * @param to           the last date to include (inclusive)
     * @param totalsByDate per-day nutrient totals, keyed by date
     * @param weightsUpTo  weight entries on or before {@code to}, ordered ascending by entry date
     * @param earliestEver the earliest ever weight entry, or null if none exists
     * @return the assembled ratio DTOs, ordered ascending by date
     */
    private List<WeightNutrientRatioDTO> buildRatios(
            LocalDate from,
            LocalDate to,
            Map<LocalDate, MacrosDTO> totalsByDate,
            List<WeightEntryEntity> weightsUpTo,
            WeightEntryEntity earliestEver) {
        final List<WeightNutrientRatioDTO> ratios = new ArrayList<>();
        final BigDecimal zeroTotal = BigDecimal.ZERO.setScale(RATIO_SCALE, RoundingMode.UNNECESSARY);
        final MacrosDTO zero = new MacrosDTO(zeroTotal, zeroTotal, zeroTotal, zeroTotal);
        int pointer = -1;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            while (pointer + 1 < weightsUpTo.size() && !weightsUpTo.get(pointer + 1).getEntryDate().isAfter(date)) {
                pointer++;
            }
            final WeightEntryEntity applicable = pointer >= 0 ? weightsUpTo.get(pointer) : earliestEver;
            final BigDecimal weightKg = applicable != null ? applicable.getWeightKg() : null;
            final MacrosDTO totals = totalsByDate.getOrDefault(date, zero);
            ratios.add(buildRatio(date, totals, weightKg));
        }
        return ratios;
    }

    /**
     * Groups meal entries by date and sums their macro-nutrient values per day.
     *
     * @param entries the meal entries to group and sum
     * @return the per-day macro totals, keyed by date
     */
    private Map<LocalDate, MacrosDTO> computeTotalsByDate(List<MealEntryEntity> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(MealEntryEntity::getEntryDate, Collectors.collectingAndThen(
                        Collectors.toList(), this::sumMacros)));
    }

    /**
     * Sums macro-nutrient values across all given meal entries.
     *
     * @param entries the entries to sum
     * @return a MacrosDTO with aggregated totals
     */
    private MacrosDTO sumMacros(List<MealEntryEntity> entries) {
        BigDecimal kcal = BigDecimal.ZERO;
        BigDecimal proteinG = BigDecimal.ZERO;
        BigDecimal carbsG = BigDecimal.ZERO;
        BigDecimal fatG = BigDecimal.ZERO;
        for (final MealEntryEntity entry : entries) {
            kcal = kcal.add(entry.getKcal());
            proteinG = proteinG.add(entry.getProteinG());
            carbsG = carbsG.add(entry.getCarbsG());
            fatG = fatG.add(entry.getFatG());
        }
        return new MacrosDTO(kcal, proteinG, carbsG, fatG);
    }

    /**
     * Builds a single ratio DTO from a day's totals and applicable weight.
     *
     * @param date     the date this entry applies to
     * @param totals   the day's nutrient totals
     * @param weightKg the applicable body weight in kilograms, or null if unavailable
     * @return the assembled ratio DTO
     */
    private WeightNutrientRatioDTO buildRatio(LocalDate date, MacrosDTO totals, BigDecimal weightKg) {
        return new WeightNutrientRatioDTO(
                date,
                weightKg,
                totals.kcal(),
                totals.proteinG(),
                totals.carbsG(),
                totals.fatG(),
                ratio(totals.kcal(), weightKg),
                ratio(totals.proteinG(), weightKg),
                ratio(totals.carbsG(), weightKg),
                ratio(totals.fatG(), weightKg)
        );
    }

    /**
     * Divides a total by the applicable body weight, rounding to {@value #RATIO_SCALE} decimal places.
     *
     * @param total    the nutrient total for the day
     * @param weightKg the applicable body weight in kilograms, or null if unavailable
     * @return the computed ratio, or null if {@code weightKg} is null
     */
    private BigDecimal ratio(BigDecimal total, BigDecimal weightKg) {
        if (weightKg == null) {
            return null;
        }
        return total.divide(weightKg, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
