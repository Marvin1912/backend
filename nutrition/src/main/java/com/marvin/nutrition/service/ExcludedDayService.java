package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.ExcludedDayDTO;
import com.marvin.nutrition.entity.ExcludedDayEntity;
import com.marvin.nutrition.repository.ExcludedDayRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Manages days excluded from nutrition evaluations (averages, ratios, adherence, PDF export). */
@Service
public class ExcludedDayService {

    private final ExcludedDayRepository excludedDayRepository;

    /**
     * Creates a new ExcludedDayService.
     *
     * @param excludedDayRepository JPA repository for excluded days
     */
    public ExcludedDayService(ExcludedDayRepository excludedDayRepository) {
        this.excludedDayRepository = excludedDayRepository;
    }

    /**
     * Returns all excluded days within the given range (inclusive), ascending by date.
     *
     * @param from the first date to include
     * @param to   the last date to include
     * @return a Mono emitting the excluded days within the range
     */
    public Mono<List<ExcludedDayDTO>> findBetween(LocalDate from, LocalDate to) {
        return Mono.fromCallable(() -> excludedDayRepository.findByEntryDateBetweenOrderByEntryDateAsc(from, to)
                        .stream()
                        .map(this::toDTO)
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Returns the set of excluded dates within the given range (inclusive). Performs blocking
     * repository access and must only be called from a blocking-friendly scheduler.
     *
     * @param from the first date to include
     * @param to   the last date to include
     * @return the excluded dates within the range
     */
    public Set<LocalDate> findExcludedDatesSync(LocalDate from, LocalDate to) {
        return excludedDayRepository.findByEntryDateBetweenOrderByEntryDateAsc(from, to).stream()
                .map(ExcludedDayEntity::getEntryDate)
                .collect(Collectors.toSet());
    }

    /**
     * Returns whether the given date is excluded. Performs blocking repository access and must
     * only be called from a blocking-friendly scheduler.
     *
     * @param date the date to check
     * @return true if the date is excluded
     */
    public boolean isExcludedSync(LocalDate date) {
        return excludedDayRepository.existsById(date);
    }

    /**
     * Excludes the given day, or updates its reason if it is already excluded.
     *
     * @param date   the day to exclude
     * @param reason optional reason, blank values are stored as null
     * @return a Mono emitting the excluded day
     */
    public Mono<ExcludedDayDTO> exclude(LocalDate date, String reason) {
        return Mono.fromCallable(() -> {
            final ExcludedDayEntity entity = excludedDayRepository.findById(date).orElseGet(() -> {
                final ExcludedDayEntity created = new ExcludedDayEntity();
                created.setEntryDate(date);
                return created;
            });
            entity.setReason(reason == null || reason.isBlank() ? null : reason.trim());
            return toDTO(excludedDayRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Re-includes the given day. Does nothing if the day is not excluded.
     *
     * @param date the day to include again
     * @return an empty Mono on completion
     */
    public Mono<Void> include(LocalDate date) {
        return Mono.fromRunnable(() -> excludedDayRepository.deleteById(date))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private ExcludedDayDTO toDTO(ExcludedDayEntity entity) {
        return new ExcludedDayDTO(entity.getEntryDate(), entity.getReason());
    }
}
