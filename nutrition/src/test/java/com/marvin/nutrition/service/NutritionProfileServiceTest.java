package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.ProfileDTO;
import com.marvin.nutrition.entity.ActivityLevel;
import com.marvin.nutrition.entity.Goal;
import com.marvin.nutrition.entity.ProfileEntity;
import com.marvin.nutrition.entity.Sex;
import com.marvin.nutrition.mapper.ProfileMapper;
import com.marvin.nutrition.repository.ProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/** Unit tests for {@link NutritionProfileService} covering the single-row profile invariant. */
@ExtendWith(MockitoExtension.class)
@DisplayName("NutritionProfileService Tests")
class NutritionProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileMapper profileMapper;

    private NutritionProfileService nutritionProfileService;

    @BeforeEach
    void setUp() {
        nutritionProfileService = new NutritionProfileService(profileRepository, profileMapper);
    }

    private ProfileDTO dto() {
        return new ProfileDTO(
                ProfileEntity.SINGLETON_ID,
                Sex.MALE,
                LocalDate.of(1990, 5, 15),
                BigDecimal.valueOf(180),
                ActivityLevel.MODERATE,
                Goal.MAINTAIN,
                BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(0.30),
                null);
    }

    @Test
    @DisplayName("getProfile returns the row found by findById(SINGLETON_ID)")
    void getProfileReturnsRowBySingletonId() {
        final ProfileEntity entity = new ProfileEntity();
        entity.setId(ProfileEntity.SINGLETON_ID);
        final ProfileDTO expected = dto();

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.of(entity));
        when(profileMapper.toDTO(entity)).thenReturn(expected);

        StepVerifier.create(nutritionProfileService.getProfile())
                .expectNext(expected)
                .verifyComplete();

        verify(profileRepository).findById(ProfileEntity.SINGLETON_ID);
        verify(profileRepository, never()).findAll();
    }

    @Test
    @DisplayName("getProfile emits NoSuchElementException when no profile exists")
    void getProfileEmitsErrorWhenMissing() {
        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        StepVerifier.create(nutritionProfileService.getProfile())
                .expectErrorMatches(error -> error instanceof NoSuchElementException
                        && "No nutrition profile has been created yet.".equals(error.getMessage()))
                .verify();
    }

    @Test
    @DisplayName("upsertProfile on empty repository creates a new entity pinned to SINGLETON_ID")
    void upsertProfileCreatesNewEntityWithSingletonId() {
        final ProfileDTO request = dto();

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.empty());
        when(profileRepository.save(any(ProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileMapper.toDTO(any(ProfileEntity.class))).thenReturn(request);

        StepVerifier.create(nutritionProfileService.upsertProfile(request))
                .expectNext(request)
                .verifyComplete();

        final ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(ProfileEntity.SINGLETON_ID, captor.getValue().getId());
    }

    @Test
    @DisplayName("upsertProfile on existing row updates that row, keeping id 1")
    void upsertProfileUpdatesExistingRow() {
        final ProfileEntity existing = new ProfileEntity();
        existing.setId(ProfileEntity.SINGLETON_ID);
        final ProfileDTO request = dto();

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(ProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileMapper.toDTO(any(ProfileEntity.class))).thenReturn(request);

        StepVerifier.create(nutritionProfileService.upsertProfile(request))
                .expectNext(request)
                .verifyComplete();

        final ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(ProfileEntity.SINGLETON_ID, captor.getValue().getId());
        verify(profileRepository, never()).findAll();
    }

    @Test
    @DisplayName("upsertProfile defaults proteinPerKg and fatPct when not provided")
    void upsertProfileAppliesDefaults() {
        final ProfileDTO request = new ProfileDTO(
                null,
                Sex.FEMALE,
                LocalDate.of(1995, 1, 1),
                BigDecimal.valueOf(165),
                ActivityLevel.LIGHT,
                Goal.CUT,
                null,
                null,
                null);

        when(profileRepository.findById(ProfileEntity.SINGLETON_ID)).thenReturn(Optional.empty());
        when(profileRepository.save(any(ProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileMapper.toDTO(any(ProfileEntity.class))).thenAnswer(invocation -> {
            final ProfileEntity saved = invocation.getArgument(0);
            return new ProfileDTO(saved.getId(), saved.getSex(), saved.getBirthDate(), saved.getHeightCm(),
                    saved.getActivityLevel(), saved.getGoal(), saved.getProteinPerKg(), saved.getFatPct(),
                    saved.getBasalKcal());
        });

        StepVerifier.create(nutritionProfileService.upsertProfile(request))
                .assertNext(saved -> {
                    assertEquals(BigDecimal.valueOf(2.0), saved.proteinPerKg());
                    assertEquals(BigDecimal.valueOf(0.30), saved.fatPct());
                })
                .verifyComplete();
    }
}
