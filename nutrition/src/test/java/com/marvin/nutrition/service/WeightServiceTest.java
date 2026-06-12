package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateWeightEntryRequest;
import com.marvin.nutrition.dto.WeightEntryDTO;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.mapper.WeightEntryMapper;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/** Unit tests for {@link WeightService} covering CRUD and day-target snapshot refresh side effects. */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeightService Tests")
class WeightServiceTest {

    @Mock
    private WeightEntryRepository weightEntryRepository;

    @Mock
    private WeightEntryMapper weightEntryMapper;

    @Mock
    private DayTargetSnapshotService dayTargetSnapshotService;

    @InjectMocks
    private WeightService weightService;

    private LocalDate today;
    private WeightEntryDTO weightEntryDTO;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 6, 7);
        weightEntryDTO = new WeightEntryDTO(1L, today, new BigDecimal("80.50"));
    }

    // -----------------------------------------------------------------------
    // create
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("create persists the entry and refreshes the day-target snapshot for the entry date")
    void create_PersistsEntryAndRefreshesSnapshotForEntryDate() {
        final CreateWeightEntryRequest request = new CreateWeightEntryRequest(today, new BigDecimal("80.50"));
        final WeightEntryEntity saved = new WeightEntryEntity();
        saved.setEntryDate(today);
        saved.setWeightKg(new BigDecimal("80.50"));

        when(weightEntryRepository.save(any(WeightEntryEntity.class))).thenReturn(saved);
        when(weightEntryMapper.toDTO(saved)).thenReturn(weightEntryDTO);

        StepVerifier.create(weightService.create(request))
                .expectNext(weightEntryDTO)
                .verifyComplete();

        verify(dayTargetSnapshotService).refreshSnapshotIfExists(today);
    }

    // -----------------------------------------------------------------------
    // update
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("update emits NoSuchElementException when entry not found")
    void update_NotFound_EmitsNoSuchElementException() {
        when(weightEntryRepository.findById(1L)).thenReturn(Optional.empty());

        final CreateWeightEntryRequest request = new CreateWeightEntryRequest(today, new BigDecimal("80.50"));

        StepVerifier.create(weightService.update(1L, request))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(weightEntryRepository, never()).save(any());
        verify(dayTargetSnapshotService, never()).refreshSnapshotIfExists(any());
    }

    @Test
    @DisplayName("update refreshes the snapshot only for the new date when the entry date is unchanged")
    void update_DateUnchanged_RefreshesSnapshotOnceForNewDate() {
        final WeightEntryEntity existing = new WeightEntryEntity();
        existing.setId(1L);
        existing.setEntryDate(today);
        existing.setWeightKg(new BigDecimal("79.00"));

        final CreateWeightEntryRequest request = new CreateWeightEntryRequest(today, new BigDecimal("80.50"));

        when(weightEntryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(weightEntryRepository.save(any(WeightEntryEntity.class))).thenReturn(existing);
        when(weightEntryMapper.toDTO(existing)).thenReturn(weightEntryDTO);

        StepVerifier.create(weightService.update(1L, request))
                .expectNext(weightEntryDTO)
                .verifyComplete();

        verify(dayTargetSnapshotService).refreshSnapshotIfExists(today);
        verify(dayTargetSnapshotService, org.mockito.Mockito.times(1)).refreshSnapshotIfExists(any());
    }

    @Test
    @DisplayName("update refreshes the snapshot for both the old and new date when the entry date changes")
    void update_DateChanged_RefreshesSnapshotForOldAndNewDate() {
        final LocalDate oldDate = today.minusDays(1);
        final WeightEntryEntity existing = new WeightEntryEntity();
        existing.setId(1L);
        existing.setEntryDate(oldDate);
        existing.setWeightKg(new BigDecimal("79.00"));

        final CreateWeightEntryRequest request = new CreateWeightEntryRequest(today, new BigDecimal("80.50"));

        when(weightEntryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(weightEntryRepository.save(any(WeightEntryEntity.class))).thenReturn(existing);
        when(weightEntryMapper.toDTO(existing)).thenReturn(weightEntryDTO);

        StepVerifier.create(weightService.update(1L, request))
                .expectNext(weightEntryDTO)
                .verifyComplete();

        verify(dayTargetSnapshotService).refreshSnapshotIfExists(today);
        verify(dayTargetSnapshotService).refreshSnapshotIfExists(oldDate);
    }

    // -----------------------------------------------------------------------
    // sanity: entity mutation reflects request values
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("update applies the new entry date and weight to the persisted entity")
    void update_AppliesNewEntryDateAndWeight() {
        final WeightEntryEntity existing = new WeightEntryEntity();
        existing.setId(1L);
        existing.setEntryDate(today.minusDays(1));
        existing.setWeightKg(new BigDecimal("79.00"));

        final CreateWeightEntryRequest request = new CreateWeightEntryRequest(today, new BigDecimal("80.50"));

        when(weightEntryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(weightEntryRepository.save(any(WeightEntryEntity.class))).thenReturn(existing);
        when(weightEntryMapper.toDTO(existing)).thenReturn(weightEntryDTO);

        StepVerifier.create(weightService.update(1L, request))
                .expectNext(weightEntryDTO)
                .verifyComplete();

        assertEquals(today, existing.getEntryDate());
        assertEquals(new BigDecimal("80.50"), existing.getWeightKg());
    }
}
