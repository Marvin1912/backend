package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateSportActivityRequest;
import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.dto.UpdateSportActivityRequest;
import com.marvin.nutrition.entity.SportActivityEntity;
import com.marvin.nutrition.entity.SportActivityType;
import com.marvin.nutrition.mapper.SportActivityMapper;
import com.marvin.nutrition.repository.SportActivityRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SportActivityWriteService} covering the transactional write operations. */
@ExtendWith(MockitoExtension.class)
@DisplayName("SportActivityWriteService Tests")
class SportActivityWriteServiceTest {

    @Mock
    private SportActivityRepository sportActivityRepository;

    @Mock
    private SportActivityMapper sportActivityMapper;

    @InjectMocks
    private SportActivityWriteService sportActivityWriteService;

    private UUID activityId;
    private SportActivityEntity sportActivityEntity;
    private SportActivityDTO sportActivityDTO;
    private LocalDate today;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        activityId = UUID.randomUUID();
        today = LocalDate.of(2026, 6, 7);

        sportActivityEntity = new SportActivityEntity();
        sportActivityEntity.setEntryDate(today);
        sportActivityEntity.setActivityType(SportActivityType.RUNNING);
        sportActivityEntity.setKcalBurned(new BigDecimal("300.00"));

        sportActivityDTO = new SportActivityDTO(
                activityId, today, SportActivityType.RUNNING, null, new BigDecimal("300.00")
        );
    }

    // -----------------------------------------------------------------------
    // createActivity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createActivity persists a non-OTHER activity without requiring a description")
    void createActivity_NonOtherType_PersistsWithoutDescription() {
        final CreateSportActivityRequest req =
                new CreateSportActivityRequest(SportActivityType.RUNNING, null, new BigDecimal("300.00"));

        when(sportActivityRepository.save(any(SportActivityEntity.class))).thenReturn(sportActivityEntity);
        when(sportActivityMapper.toDTO(sportActivityEntity)).thenReturn(sportActivityDTO);

        final SportActivityDTO result = sportActivityWriteService.createActivity(today, req);

        assertEquals(sportActivityDTO, result);
        verify(sportActivityRepository).save(argThat(e ->
                SportActivityType.RUNNING.equals(e.getActivityType())
                        && today.equals(e.getEntryDate())
                        && new BigDecimal("300.00").compareTo(e.getKcalBurned()) == 0
        ));
    }

    @Test
    @DisplayName("createActivity persists a CROSS_TRAINER activity without requiring a description")
    void createActivity_CrossTrainerType_PersistsWithoutDescription() {
        final CreateSportActivityRequest req =
                new CreateSportActivityRequest(SportActivityType.CROSS_TRAINER, null, new BigDecimal("250.00"));

        final SportActivityEntity saved = new SportActivityEntity();
        saved.setEntryDate(today);
        saved.setActivityType(SportActivityType.CROSS_TRAINER);
        saved.setKcalBurned(new BigDecimal("250.00"));

        final SportActivityDTO dto = new SportActivityDTO(
                activityId, today, SportActivityType.CROSS_TRAINER, null, new BigDecimal("250.00")
        );

        when(sportActivityRepository.save(any(SportActivityEntity.class))).thenReturn(saved);
        when(sportActivityMapper.toDTO(saved)).thenReturn(dto);

        final SportActivityDTO result = sportActivityWriteService.createActivity(today, req);

        assertEquals(dto, result);
        verify(sportActivityRepository).save(argThat(e ->
                SportActivityType.CROSS_TRAINER.equals(e.getActivityType())
                        && today.equals(e.getEntryDate())
                        && new BigDecimal("250.00").compareTo(e.getKcalBurned()) == 0
        ));
    }

    @Test
    @DisplayName("createActivity persists an OTHER activity when description is supplied")
    void createActivity_OtherTypeWithDescription_Persists() {
        final CreateSportActivityRequest req =
                new CreateSportActivityRequest(SportActivityType.OTHER, "Climbing", new BigDecimal("400.00"));

        final SportActivityEntity saved = new SportActivityEntity();
        saved.setEntryDate(today);
        saved.setActivityType(SportActivityType.OTHER);
        saved.setDescription("Climbing");
        saved.setKcalBurned(new BigDecimal("400.00"));

        final SportActivityDTO dto = new SportActivityDTO(
                UUID.randomUUID(), today, SportActivityType.OTHER, "Climbing", new BigDecimal("400.00")
        );

        when(sportActivityRepository.save(any(SportActivityEntity.class))).thenReturn(saved);
        when(sportActivityMapper.toDTO(saved)).thenReturn(dto);

        final SportActivityDTO result = sportActivityWriteService.createActivity(today, req);

        assertEquals(dto, result);
        verify(sportActivityRepository).save(argThat(e -> "Climbing".equals(e.getDescription())));
    }

    @Test
    @DisplayName("createActivity throws IllegalArgumentException when OTHER type has no description")
    void createActivity_OtherTypeMissingDescription_ThrowsIllegalArgumentException() {
        final CreateSportActivityRequest req =
                new CreateSportActivityRequest(SportActivityType.OTHER, null, new BigDecimal("400.00"));

        assertThrows(IllegalArgumentException.class,
                () -> sportActivityWriteService.createActivity(today, req));

        verify(sportActivityRepository, never()).save(any());
    }

    @Test
    @DisplayName("createActivity throws IllegalArgumentException when OTHER type has blank description")
    void createActivity_OtherTypeBlankDescription_ThrowsIllegalArgumentException() {
        final CreateSportActivityRequest req =
                new CreateSportActivityRequest(SportActivityType.OTHER, "   ", new BigDecimal("400.00"));

        assertThrows(IllegalArgumentException.class,
                () -> sportActivityWriteService.createActivity(today, req));

        verify(sportActivityRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // updateActivity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateActivity applies non-null fields and persists the change")
    void updateActivity_NonNullFields_AppliesAndPersists() {
        final UpdateSportActivityRequest req =
                new UpdateSportActivityRequest(SportActivityType.CYCLING, null, new BigDecimal("500.00"));

        final SportActivityEntity updated = new SportActivityEntity();
        updated.setEntryDate(today);
        updated.setActivityType(SportActivityType.CYCLING);
        updated.setKcalBurned(new BigDecimal("500.00"));

        final SportActivityDTO updatedDTO = new SportActivityDTO(
                activityId, today, SportActivityType.CYCLING, null, new BigDecimal("500.00")
        );

        when(sportActivityRepository.findById(activityId)).thenReturn(Optional.of(sportActivityEntity));
        when(sportActivityRepository.save(any(SportActivityEntity.class))).thenReturn(updated);
        when(sportActivityMapper.toDTO(updated)).thenReturn(updatedDTO);

        final SportActivityDTO result = sportActivityWriteService.updateActivity(activityId, req);

        assertEquals(updatedDTO, result);
        verify(sportActivityRepository).save(argThat(e ->
                SportActivityType.CYCLING.equals(e.getActivityType())
                        && new BigDecimal("500.00").compareTo(e.getKcalBurned()) == 0
        ));
    }

    @Test
    @DisplayName("updateActivity throws NoSuchElementException when activity not found")
    void updateActivity_NotFound_ThrowsNoSuchElementException() {
        final UpdateSportActivityRequest req =
                new UpdateSportActivityRequest(SportActivityType.CYCLING, null, null);

        when(sportActivityRepository.findById(activityId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> sportActivityWriteService.updateActivity(activityId, req));

        verify(sportActivityRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateActivity throws IllegalArgumentException when resulting type is OTHER without description")
    void updateActivity_ResultingOtherTypeMissingDescription_ThrowsIllegalArgumentException() {
        final UpdateSportActivityRequest req =
                new UpdateSportActivityRequest(SportActivityType.OTHER, null, null);

        when(sportActivityRepository.findById(activityId)).thenReturn(Optional.of(sportActivityEntity));

        assertThrows(IllegalArgumentException.class,
                () -> sportActivityWriteService.updateActivity(activityId, req));

        verify(sportActivityRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateActivity allows switching to OTHER type when description is supplied in the same request")
    void updateActivity_ResultingOtherTypeWithDescription_Persists() {
        final UpdateSportActivityRequest req =
                new UpdateSportActivityRequest(SportActivityType.OTHER, "Climbing", null);

        final SportActivityEntity updated = new SportActivityEntity();
        updated.setEntryDate(today);
        updated.setActivityType(SportActivityType.OTHER);
        updated.setDescription("Climbing");
        updated.setKcalBurned(new BigDecimal("300.00"));

        final SportActivityDTO updatedDTO = new SportActivityDTO(
                activityId, today, SportActivityType.OTHER, "Climbing", new BigDecimal("300.00")
        );

        when(sportActivityRepository.findById(activityId)).thenReturn(Optional.of(sportActivityEntity));
        when(sportActivityRepository.save(any(SportActivityEntity.class))).thenReturn(updated);
        when(sportActivityMapper.toDTO(updated)).thenReturn(updatedDTO);

        final SportActivityDTO result = sportActivityWriteService.updateActivity(activityId, req);

        assertEquals(updatedDTO, result);
    }

    // -----------------------------------------------------------------------
    // deleteActivity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteActivity throws NoSuchElementException when activity not found")
    void deleteActivity_NotFound_ThrowsNoSuchElementException() {
        when(sportActivityRepository.findById(activityId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> sportActivityWriteService.deleteActivity(activityId));

        verify(sportActivityRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteActivity deletes when activity exists")
    void deleteActivity_Exists_Deletes() {
        when(sportActivityRepository.findById(activityId)).thenReturn(Optional.of(sportActivityEntity));

        sportActivityWriteService.deleteActivity(activityId);

        verify(sportActivityRepository).delete(sportActivityEntity);
    }
}
