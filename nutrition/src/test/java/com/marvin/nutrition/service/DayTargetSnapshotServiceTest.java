package com.marvin.nutrition.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import com.marvin.nutrition.repository.DayTargetSnapshotRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link DayTargetSnapshotService}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("DayTargetSnapshotService Tests")
class DayTargetSnapshotServiceTest {

    @Mock
    private DayTargetSnapshotRepository dayTargetSnapshotRepository;

    @Mock
    private NutritionTargetService nutritionTargetService;

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
    @DisplayName("ensureSnapshot does nothing when a snapshot already exists")
    void ensureSnapshot_ExistingSnapshot_DoesNothing() {
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.of(new DayTargetSnapshotEntity()));

        dayTargetSnapshotService.ensureSnapshot(today);

        verify(dayTargetSnapshotRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(nutritionTargetService, never()).getTargetsSync(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("ensureSnapshot does nothing when targets cannot be computed")
    void ensureSnapshot_TargetsUnavailable_DoesNothing() {
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.empty());
        when(nutritionTargetService.getTargetsSync(today)).thenThrow(new TargetCalculationException("No profile"));

        dayTargetSnapshotService.ensureSnapshot(today);

        verify(dayTargetSnapshotRepository, never()).save(org.mockito.ArgumentMatchers.any());
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

        verify(dayTargetSnapshotRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(nutritionTargetService, never()).getTargetsSync(org.mockito.ArgumentMatchers.any());
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

        verify(dayTargetSnapshotRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
