package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.repository.DayTargetSnapshotRepository;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Unit tests for {@link DayTargetSnapshotService}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("DayTargetSnapshotService Tests")
class DayTargetSnapshotServiceTest {

    @Mock
    private DayTargetSnapshotRepository dayTargetSnapshotRepository;

    @Mock
    private NutritionTargetService nutritionTargetService;

    @Mock
    private WeightEntryRepository weightEntryRepository;

    @InjectMocks
    private DayTargetSnapshotService dayTargetSnapshotService;

    private LocalDate today;
    private TargetsDTO targets;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 6, 7);
        targets = new TargetsDTO(1750, 2160, 2160, 160, 72, 252, "MIFFLIN_ST_JEOR");
    }

    // -----------------------------------------------------------------------
    // ensureSnapshot
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ensureSnapshot persists a snapshot when none exists and targets can be computed")
    void ensureSnapshot_NoExistingSnapshot_PersistsSnapshotFromComputedTargets() {
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.empty());
        doReturn(targets).when(nutritionTargetService).getTargetsSync(today);

        dayTargetSnapshotService.ensureSnapshot(today);

        verify(dayTargetSnapshotRepository).saveAndFlush(argThat(snapshot ->
                today.equals(snapshot.getEntryDate())
                        && snapshot.getBmr() == 1750
                        && snapshot.getMaintenanceKcal() == 2160
                        && snapshot.getTargetKcal() == 2160
                        && snapshot.getProteinG() == 160
                        && snapshot.getFatG() == 72
                        && snapshot.getCarbsG() == 252
                        && "MIFFLIN_ST_JEOR".equals(snapshot.getBasis())
        ));
    }

    @Test
    @DisplayName("ensureSnapshot does nothing when a snapshot already exists")
    void ensureSnapshot_ExistingSnapshot_DoesNothing() {
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.of(new DayTargetSnapshotEntity()));

        dayTargetSnapshotService.ensureSnapshot(today);

        verify(dayTargetSnapshotRepository, never()).saveAndFlush(any());
        verify(nutritionTargetService, never()).getTargetsSync(any());
    }

    @Test
    @DisplayName("ensureSnapshot does nothing when targets cannot be computed")
    void ensureSnapshot_TargetsUnavailable_DoesNothing() {
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.empty());
        when(nutritionTargetService.getTargetsSync(today)).thenThrow(new TargetCalculationException("No profile"));

        dayTargetSnapshotService.ensureSnapshot(today);

        verify(dayTargetSnapshotRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("ensureSnapshot completes normally when saveAndFlush throws DataIntegrityViolationException "
            + "due to a concurrent insert for the same date")
    void ensureSnapshot_ConcurrentDuplicateInsert_CompletesNormally() {
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.empty());
        doReturn(targets).when(nutritionTargetService).getTargetsSync(today);
        when(dayTargetSnapshotRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertDoesNotThrow(() -> dayTargetSnapshotService.ensureSnapshot(today));
    }

    // -----------------------------------------------------------------------
    // refreshSnapshotIfExists
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("refreshSnapshotIfExists overwrites an existing snapshot with freshly computed targets")
    void refreshSnapshotIfExists_ExistingSnapshot_OverwritesWithComputedTargets() {
        final DayTargetSnapshotEntity existing = new DayTargetSnapshotEntity();
        existing.setEntryDate(today);
        existing.setBmr(1700);
        existing.setMaintenanceKcal(2100);
        existing.setTargetKcal(2000);
        existing.setProteinG(150);
        existing.setFatG(67);
        existing.setCarbsG(248);
        existing.setBasis("BASAL_KCAL");

        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.of(existing));
        doReturn(targets).when(nutritionTargetService).getTargetsSync(today);

        dayTargetSnapshotService.refreshSnapshotIfExists(today);

        verify(dayTargetSnapshotRepository).save(argThat(snapshot ->
                today.equals(snapshot.getEntryDate())
                        && snapshot.getBmr() == 1750
                        && snapshot.getMaintenanceKcal() == 2160
                        && snapshot.getTargetKcal() == 2160
                        && snapshot.getProteinG() == 160
                        && snapshot.getFatG() == 72
                        && snapshot.getCarbsG() == 252
                        && "MIFFLIN_ST_JEOR".equals(snapshot.getBasis())
        ));
    }

    @Test
    @DisplayName("refreshSnapshotIfExists does nothing when no snapshot exists for the date")
    void refreshSnapshotIfExists_NoSnapshot_DoesNothing() {
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.empty());

        dayTargetSnapshotService.refreshSnapshotIfExists(today);

        verify(dayTargetSnapshotRepository, never()).save(any());
        verify(nutritionTargetService, never()).getTargetsSync(any());
    }

    @Test
    @DisplayName("refreshSnapshotIfExists leaves the existing snapshot untouched when targets cannot be computed")
    void refreshSnapshotIfExists_TargetsUnavailable_LeavesExistingSnapshotUntouched() {
        final DayTargetSnapshotEntity existing = new DayTargetSnapshotEntity();
        existing.setEntryDate(today);
        existing.setBmr(1700);
        existing.setMaintenanceKcal(2100);
        existing.setTargetKcal(2000);
        existing.setProteinG(150);
        existing.setFatG(67);
        existing.setCarbsG(248);
        existing.setBasis("BASAL_KCAL");

        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.of(existing));
        when(nutritionTargetService.getTargetsSync(today)).thenThrow(new TargetCalculationException("No profile"));

        dayTargetSnapshotService.refreshSnapshotIfExists(today);

        verify(dayTargetSnapshotRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // refreshSnapshotsFrom
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("refreshSnapshotsFrom refreshes all existing snapshots from the given date up to "
            + "(but not including) the next weight entry's date")
    void refreshSnapshotsFrom_NextWeightEntryExists_RefreshesSnapshotsUpToNextWeightDate() {
        final LocalDate from = today;
        final LocalDate nextWeightDate = today.plusDays(5);
        final WeightEntryEntity nextWeightEntry = new WeightEntryEntity();
        nextWeightEntry.setEntryDate(nextWeightDate);

        final DayTargetSnapshotEntity snapshotDay1 = new DayTargetSnapshotEntity();
        snapshotDay1.setEntryDate(today.plusDays(1));
        final DayTargetSnapshotEntity snapshotDay3 = new DayTargetSnapshotEntity();
        snapshotDay3.setEntryDate(today.plusDays(3));

        when(weightEntryRepository.findTopByEntryDateGreaterThanOrderByEntryDateAsc(from))
                .thenReturn(Optional.of(nextWeightEntry));
        when(dayTargetSnapshotRepository.findByEntryDateBetween(from, nextWeightDate.minusDays(1)))
                .thenReturn(List.of(snapshotDay1, snapshotDay3));
        doReturn(targets).when(nutritionTargetService).getTargetsSync(any(LocalDate.class));

        dayTargetSnapshotService.refreshSnapshotsFrom(from);

        verify(dayTargetSnapshotRepository).save(argThat(snapshot ->
                today.plusDays(1).equals(snapshot.getEntryDate()) && snapshot.getBmr() == 1750));
        verify(dayTargetSnapshotRepository).save(argThat(snapshot ->
                today.plusDays(3).equals(snapshot.getEntryDate()) && snapshot.getBmr() == 1750));
        verify(dayTargetSnapshotRepository, never()).findByEntryDateGreaterThanEqual(any());
    }

    @Test
    @DisplayName("refreshSnapshotsFrom refreshes all existing snapshots from the given date onward "
            + "when there is no later weight entry")
    void refreshSnapshotsFrom_NoNextWeightEntry_RefreshesSnapshotsToEndOfRange() {
        final LocalDate from = today;
        final DayTargetSnapshotEntity snapshotFar = new DayTargetSnapshotEntity();
        snapshotFar.setEntryDate(today.plusDays(30));

        when(weightEntryRepository.findTopByEntryDateGreaterThanOrderByEntryDateAsc(from))
                .thenReturn(Optional.empty());
        when(dayTargetSnapshotRepository.findByEntryDateGreaterThanEqual(from))
                .thenReturn(List.of(snapshotFar));
        doReturn(targets).when(nutritionTargetService).getTargetsSync(any(LocalDate.class));

        dayTargetSnapshotService.refreshSnapshotsFrom(from);

        verify(dayTargetSnapshotRepository).save(argThat(snapshot ->
                today.plusDays(30).equals(snapshot.getEntryDate()) && snapshot.getBmr() == 1750));
        verify(dayTargetSnapshotRepository, never()).findByEntryDateBetween(any(), any());
    }

    @Test
    @DisplayName("refreshSnapshotsFrom does nothing when there are no existing snapshots in range")
    void refreshSnapshotsFrom_NoSnapshotsInRange_DoesNothing() {
        final LocalDate from = today;

        when(weightEntryRepository.findTopByEntryDateGreaterThanOrderByEntryDateAsc(from))
                .thenReturn(Optional.empty());
        when(dayTargetSnapshotRepository.findByEntryDateGreaterThanEqual(from)).thenReturn(List.of());

        dayTargetSnapshotService.refreshSnapshotsFrom(from);

        verify(dayTargetSnapshotRepository, never()).save(any());
        verify(nutritionTargetService, never()).getTargetsSync(any());
    }

    @Test
    @DisplayName("refreshSnapshotsFrom leaves a snapshot untouched when targets cannot be computed for its date")
    void refreshSnapshotsFrom_TargetsUnavailableForDate_LeavesThatSnapshotUntouched() {
        final LocalDate from = today;
        final DayTargetSnapshotEntity snapshotDay1 = new DayTargetSnapshotEntity();
        snapshotDay1.setEntryDate(today.plusDays(1));
        final DayTargetSnapshotEntity snapshotDay2 = new DayTargetSnapshotEntity();
        snapshotDay2.setEntryDate(today.plusDays(2));

        when(weightEntryRepository.findTopByEntryDateGreaterThanOrderByEntryDateAsc(from))
                .thenReturn(Optional.empty());
        when(dayTargetSnapshotRepository.findByEntryDateGreaterThanEqual(from))
                .thenReturn(List.of(snapshotDay1, snapshotDay2));
        when(nutritionTargetService.getTargetsSync(today.plusDays(1)))
                .thenThrow(new TargetCalculationException("No profile"));
        doReturn(targets).when(nutritionTargetService).getTargetsSync(today.plusDays(2));

        dayTargetSnapshotService.refreshSnapshotsFrom(from);

        verify(dayTargetSnapshotRepository, never()).save(argThat(snapshot ->
                today.plusDays(1).equals(snapshot.getEntryDate())));
        verify(dayTargetSnapshotRepository).save(argThat(snapshot ->
                today.plusDays(2).equals(snapshot.getEntryDate())));
    }

    // -----------------------------------------------------------------------
    // refreshAllSnapshots
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("refreshAllSnapshots refreshes every existing snapshot")
    void refreshAllSnapshots_RefreshesEveryExistingSnapshot() {
        final DayTargetSnapshotEntity snapshot1 = new DayTargetSnapshotEntity();
        snapshot1.setEntryDate(today);
        final DayTargetSnapshotEntity snapshot2 = new DayTargetSnapshotEntity();
        snapshot2.setEntryDate(today.plusDays(10));

        when(dayTargetSnapshotRepository.findAll()).thenReturn(List.of(snapshot1, snapshot2));
        doReturn(targets).when(nutritionTargetService).getTargetsSync(any(LocalDate.class));

        dayTargetSnapshotService.refreshAllSnapshots();

        verify(dayTargetSnapshotRepository).save(argThat(snapshot ->
                today.equals(snapshot.getEntryDate()) && snapshot.getBmr() == 1750));
        verify(dayTargetSnapshotRepository).save(argThat(snapshot ->
                today.plusDays(10).equals(snapshot.getEntryDate()) && snapshot.getBmr() == 1750));
    }

    @Test
    @DisplayName("refreshAllSnapshots leaves a snapshot untouched when targets cannot be computed for its date")
    void refreshAllSnapshots_TargetsUnavailableForDate_LeavesThatSnapshotUntouched() {
        final DayTargetSnapshotEntity snapshot1 = new DayTargetSnapshotEntity();
        snapshot1.setEntryDate(today);

        when(dayTargetSnapshotRepository.findAll()).thenReturn(List.of(snapshot1));
        when(nutritionTargetService.getTargetsSync(today)).thenThrow(new TargetCalculationException("No profile"));

        dayTargetSnapshotService.refreshAllSnapshots();

        verify(dayTargetSnapshotRepository, never()).save(any());
    }

    @Test
    @DisplayName("refreshAllSnapshots does nothing when no snapshots exist")
    void refreshAllSnapshots_NoSnapshots_DoesNothing() {
        when(dayTargetSnapshotRepository.findAll()).thenReturn(List.of());

        dayTargetSnapshotService.refreshAllSnapshots();

        verify(dayTargetSnapshotRepository, never()).save(any());
        verify(nutritionTargetService, never()).getTargetsSync(any());
    }
}
