package com.marvin.nutrition.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.ProfileEntity;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.repository.ProfileRepository;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/** Unit tests for {@link NutritionTargetService} covering the single-row profile lookup. */
@ExtendWith(MockitoExtension.class)
@DisplayName("NutritionTargetService Tests")
class NutritionTargetServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private WeightEntryRepository weightEntryRepository;

    @Mock
    private TargetService targetService;

    private NutritionTargetService nutritionTargetService;

    @BeforeEach
    void setUp() {
        nutritionTargetService = new NutritionTargetService(profileRepository, weightEntryRepository, targetService);
    }

    @Test
    @DisplayName("getTargets loads the profile via findById(SINGLETON_ID)")
    void getTargetsLoadsProfileBySingletonId() {
        final ProfileEntity profile = new ProfileEntity();
        profile.setId(ProfileEntity.SINGLETON_ID);
        final WeightEntryEntity weight = new WeightEntryEntity();
        weight.setWeightKg(BigDecimal.valueOf(80));
        final TargetsDTO targets = new TargetsDTO(1750, 2713, 2213, 160, 74, 248, "MIFFLIN_ST_JEOR");

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.of(profile));
        when(weightEntryRepository.findTopByOrderByEntryDateDesc()).thenReturn(Optional.of(weight));
        when(targetService.computeTargets(profile, weight)).thenReturn(targets);

        StepVerifier.create(nutritionTargetService.getTargets())
                .expectNext(targets)
                .verifyComplete();

        verify(profileRepository).findById(ProfileEntity.SINGLETON_ID);
        verify(profileRepository, never()).findAll();
    }

    @Test
    @DisplayName("getTargets passes a null profile to the calculation when none exists")
    void getTargetsPassesNullProfileWhenAbsent() {
        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.empty());
        when(weightEntryRepository.findTopByOrderByEntryDateDesc()).thenReturn(Optional.empty());
        when(targetService.computeTargets(isNull(), isNull()))
                .thenThrow(new TargetCalculationException("No nutrition profile found. Please create a profile first."));

        StepVerifier.create(nutritionTargetService.getTargets())
                .expectErrorMatches(error -> error instanceof TargetCalculationException)
                .verify();

        verify(targetService).computeTargets(eq(null), eq(null));
    }
}
