package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.ProfileDTO;
import com.marvin.nutrition.entity.ProfileEntity;
import com.marvin.nutrition.mapper.ProfileMapper;
import com.marvin.nutrition.repository.ProfileRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates reads and upserts of the single-row nutrition profile. */
@Service
public class NutritionProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    /**
     * Creates a new NutritionProfileService.
     *
     * @param profileRepository JPA repository for the profile row
     * @param profileMapper     MapStruct mapper for entity/DTO conversion
     */
    public NutritionProfileService(ProfileRepository profileRepository, ProfileMapper profileMapper) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
    }

    /**
     * Returns the current profile, or a {@link NoSuchElementException} if none has been created yet.
     *
     * @return a Mono emitting the profile DTO
     */
    public Mono<ProfileDTO> getProfile() {
        return Mono.fromCallable(() -> profileRepository.findById(ProfileEntity.SINGLETON_ID)
                .map(profileMapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("No nutrition profile has been created yet.")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates or replaces the single nutrition profile row.
     *
     * @param dto the profile data to persist
     * @return a Mono emitting the saved profile DTO
     */
    public Mono<ProfileDTO> upsertProfile(ProfileDTO dto) {
        return Mono.fromCallable(() -> {
            final ProfileEntity entity = profileRepository.findById(ProfileEntity.SINGLETON_ID)
                    .orElseGet(ProfileEntity::new);
            entity.setId(ProfileEntity.SINGLETON_ID);
            entity.setSex(dto.sex());
            entity.setBirthDate(dto.birthDate());
            entity.setHeightCm(dto.heightCm());
            entity.setActivityLevel(dto.activityLevel());
            entity.setGoal(dto.goal());
            entity.setProteinPerKg(dto.proteinPerKg() != null ? dto.proteinPerKg() : java.math.BigDecimal.valueOf(2.0));
            entity.setFatPct(dto.fatPct() != null ? dto.fatPct() : java.math.BigDecimal.valueOf(0.30));
            entity.setBasalKcal(dto.basalKcal());
            return profileMapper.toDTO(profileRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
