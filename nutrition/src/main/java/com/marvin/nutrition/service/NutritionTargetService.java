package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.ProfileEntity;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.repository.ProfileRepository;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrating service that loads profile and latest weight, then delegates to {@link TargetService}. */
@Service
public class NutritionTargetService {

    private final ProfileRepository profileRepository;
    private final WeightEntryRepository weightEntryRepository;
    private final TargetService targetService;

    /**
     * Creates a new NutritionTargetService.
     *
     * @param profileRepository     JPA repository for the profile row
     * @param weightEntryRepository JPA repository for weight entries
     * @param targetService         pure calculation service
     */
    public NutritionTargetService(
            ProfileRepository profileRepository,
            WeightEntryRepository weightEntryRepository,
            TargetService targetService) {
        this.profileRepository = profileRepository;
        this.weightEntryRepository = weightEntryRepository;
        this.targetService = targetService;
    }

    /**
     * Loads the current profile and latest weight entry, computes and returns daily nutrition targets.
     * Emits {@link TargetCalculationException} if profile or weight data is missing.
     *
     * @return a Mono emitting the computed targets DTO
     */
    public Mono<TargetsDTO> getTargets() {
        return Mono.fromCallable(() -> {
            final ProfileEntity profile = profileRepository.findById(ProfileEntity.SINGLETON_ID).orElse(null);
            final WeightEntryEntity latestWeight = weightEntryRepository.findTopByOrderByEntryDateDesc().orElse(null);
            return targetService.computeTargets(profile, latestWeight);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Loads the current profile and the weight entry applicable as of the given date, computes and
     * returns the daily nutrition targets that applied on that date.
     *
     * <p>The applicable weight is the most recent entry on or before {@code date}; if none exists
     * (e.g. {@code date} is before the first ever weight entry), the latest known weight entry is
     * used as a fallback so the calculation can still proceed.</p>
     *
     * <p>Emits {@link TargetCalculationException} if profile or weight data is missing entirely.</p>
     *
     * @param date the date to compute applicable targets for
     * @return a Mono emitting the computed targets DTO
     */
    public Mono<TargetsDTO> getTargets(LocalDate date) {
        return Mono.fromCallable(() -> getTargetsSync(date)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Loads the current profile and the weight entry applicable as of the given date, then computes
     * and returns the daily nutrition targets that applied on that date, synchronously.
     *
     * <p>The applicable weight is the most recent entry on or before {@code date}; if none exists
     * (e.g. {@code date} is before the first ever weight entry), the latest known weight entry is
     * used as a fallback so the calculation can still proceed.</p>
     *
     * <p>Throws {@link TargetCalculationException} if profile or weight data is missing entirely.</p>
     *
     * <p>This method performs blocking repository access and must only be called from a thread
     * already running on a blocking-friendly scheduler (e.g. {@link Schedulers#boundedElastic()}).</p>
     *
     * @param date the date to compute applicable targets for
     * @return the computed targets DTO
     */
    public TargetsDTO getTargetsSync(LocalDate date) {
        final ProfileEntity profile = profileRepository.findById(ProfileEntity.SINGLETON_ID).orElse(null);
        final WeightEntryEntity applicableWeight = weightEntryRepository
                .findTopByEntryDateLessThanEqualOrderByEntryDateDesc(date)
                .or(weightEntryRepository::findTopByOrderByEntryDateDesc)
                .orElse(null);
        return targetService.computeTargets(profile, applicableWeight, date);
    }
}
