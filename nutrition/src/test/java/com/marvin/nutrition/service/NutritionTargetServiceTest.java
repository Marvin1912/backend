package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.time.LocalDate;
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

    // -----------------------------------------------------------------------
    // getTargets(LocalDate date)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTargets(date) uses the weight applicable as of that date and computes age as of that date")
    void getTargetsForDateUsesApplicableWeightAndDate() {
        final LocalDate date = LocalDate.of(2026, 1, 15);
        final ProfileEntity profile = new ProfileEntity();
        profile.setId(ProfileEntity.SINGLETON_ID);
        final WeightEntryEntity weight = new WeightEntryEntity();
        weight.setWeightKg(BigDecimal.valueOf(78));
        final TargetsDTO targets = new TargetsDTO(1700, 2100, 2100, 150, 70, 250, "MIFFLIN_ST_JEOR");

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.of(profile));
        when(weightEntryRepository.findTopByEntryDateLessThanEqualOrderByEntryDateDesc(date))
                .thenReturn(Optional.of(weight));
        when(targetService.computeTargets(profile, weight, date)).thenReturn(targets);

        StepVerifier.create(nutritionTargetService.getTargets(date))
                .expectNext(targets)
                .verifyComplete();

        verify(weightEntryRepository).findTopByEntryDateLessThanEqualOrderByEntryDateDesc(date);
        verify(weightEntryRepository, never()).findTopByOrderByEntryDateDesc();
    }

    @Test
    @DisplayName("getTargets(date) falls back to the latest weight entry when none exists on/before that date")
    void getTargetsForDateFallsBackToLatestWeightWhenNoneBeforeDate() {
        final LocalDate date = LocalDate.of(2020, 1, 1);
        final ProfileEntity profile = new ProfileEntity();
        profile.setId(ProfileEntity.SINGLETON_ID);
        final WeightEntryEntity latestWeight = new WeightEntryEntity();
        latestWeight.setWeightKg(BigDecimal.valueOf(80));
        final TargetsDTO targets = new TargetsDTO(1750, 2160, 2160, 160, 72, 252, "MIFFLIN_ST_JEOR");

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.of(profile));
        when(weightEntryRepository.findTopByEntryDateLessThanEqualOrderByEntryDateDesc(date))
                .thenReturn(Optional.empty());
        when(weightEntryRepository.findTopByOrderByEntryDateDesc()).thenReturn(Optional.of(latestWeight));
        when(targetService.computeTargets(profile, latestWeight, date)).thenReturn(targets);

        StepVerifier.create(nutritionTargetService.getTargets(date))
                .expectNext(targets)
                .verifyComplete();

        verify(weightEntryRepository).findTopByOrderByEntryDateDesc();
    }

    // -----------------------------------------------------------------------
    // getTargetsSync(LocalDate date)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTargetsSync computes targets synchronously using the weight applicable as of that date")
    void getTargetsSyncUsesApplicableWeightAndDate() {
        final LocalDate date = LocalDate.of(2026, 1, 15);
        final ProfileEntity profile = new ProfileEntity();
        profile.setId(ProfileEntity.SINGLETON_ID);
        final WeightEntryEntity weight = new WeightEntryEntity();
        weight.setWeightKg(BigDecimal.valueOf(78));
        final TargetsDTO targets = new TargetsDTO(1700, 2100, 2100, 150, 70, 250, "MIFFLIN_ST_JEOR");

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.of(profile));
        when(weightEntryRepository.findTopByEntryDateLessThanEqualOrderByEntryDateDesc(date))
                .thenReturn(Optional.of(weight));
        when(targetService.computeTargets(profile, weight, date)).thenReturn(targets);

        assertEquals(targets, nutritionTargetService.getTargetsSync(date));

        verify(weightEntryRepository, never()).findTopByOrderByEntryDateDesc();
    }

    @Test
    @DisplayName("getTargetsSync throws TargetCalculationException when profile or weight data is missing")
    void getTargetsSyncThrowsWhenDataMissing() {
        final LocalDate date = LocalDate.of(2026, 1, 15);

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.empty());
        when(weightEntryRepository.findTopByEntryDateLessThanEqualOrderByEntryDateDesc(date))
                .thenReturn(Optional.empty());
        when(weightEntryRepository.findTopByOrderByEntryDateDesc()).thenReturn(Optional.empty());
        when(targetService.computeTargets(isNull(), isNull(), eq(date)))
                .thenThrow(new TargetCalculationException("No nutrition profile found. Please create a profile first."));

        assertThrows(TargetCalculationException.class, () -> nutritionTargetService.getTargetsSync(date));
    }
}
