package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import com.marvin.nutrition.repository.DayTargetSnapshotRepository;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns read/write logic for per-day nutrition target snapshots.
 *
 * <p>All methods in this service perform blocking repository access and must only be called from
 * a thread already running on a blocking-friendly scheduler (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}).</p>
 */
@Service
public class DayTargetSnapshotService {

    private final DayTargetSnapshotRepository dayTargetSnapshotRepository;
    private final NutritionTargetService nutritionTargetService;

    /**
     * Creates a new DayTargetSnapshotService.
     *
     * @param dayTargetSnapshotRepository JPA repository for per-day nutrition target snapshots
     * @param nutritionTargetService      service for computing daily nutrition targets
     */
    public DayTargetSnapshotService(
            DayTargetSnapshotRepository dayTargetSnapshotRepository,
            NutritionTargetService nutritionTargetService) {
        this.dayTargetSnapshotRepository = dayTargetSnapshotRepository;
        this.nutritionTargetService = nutritionTargetService;
    }

    /**
     * Persists a {@link DayTargetSnapshotEntity} for the given date if one does not already exist
     * and the daily nutrition targets can currently be computed. Silently does nothing if a
     * snapshot already exists or if targets cannot be computed yet (e.g. no profile/weight data).
     *
     * <p>Runs in its own {@code REQUIRES_NEW} transaction so that a unique-constraint violation
     * caused by a concurrent insert for the same date only aborts this nested transaction,
     * leaving the caller's transaction unaffected. Such a violation is treated as success, since
     * it means a concurrent request already created the snapshot for this date.</p>
     *
     * @param date the date to snapshot targets for
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureSnapshot(LocalDate date) {
        if (dayTargetSnapshotRepository.findById(date).isPresent()) {
            return;
        }
        final TargetsDTO targets;
        try {
            targets = nutritionTargetService.getTargetsSync(date);
        } catch (TargetCalculationException e) {
            return;
        }
        try {
            dayTargetSnapshotRepository.saveAndFlush(toSnapshot(new DayTargetSnapshotEntity(), date, targets));
        } catch (DataIntegrityViolationException e) {
            // A concurrent request already created the snapshot for this date; treat as success.
        }
    }

    /**
     * Recomputes and overwrites the {@link DayTargetSnapshotEntity} for the given date if one
     * already exists. Silently leaves the existing snapshot untouched if targets cannot currently
     * be computed (e.g. no profile/weight data). Does nothing if no snapshot exists for the date.
     *
     * @param date the date whose snapshot should be refreshed
     */
    public void refreshSnapshotIfExists(LocalDate date) {
        final DayTargetSnapshotEntity existing = dayTargetSnapshotRepository.findById(date).orElse(null);
        if (existing == null) {
            return;
        }
        final TargetsDTO targets;
        try {
            targets = nutritionTargetService.getTargetsSync(date);
        } catch (TargetCalculationException e) {
            return;
        }
        dayTargetSnapshotRepository.save(toSnapshot(existing, date, targets));
    }

    /**
     * Populates the given snapshot entity's fields from the computed targets.
     *
     * @param snapshot the snapshot entity to populate (new or existing)
     * @param date     the date the snapshot applies to
     * @param targets  the computed nutrition targets
     * @return the populated snapshot entity
     */
    private DayTargetSnapshotEntity toSnapshot(DayTargetSnapshotEntity snapshot, LocalDate date, TargetsDTO targets) {
        snapshot.setEntryDate(date);
        snapshot.setBmr(targets.bmr());
        snapshot.setMaintenanceKcal(targets.maintenanceKcal());
        snapshot.setTargetKcal(targets.targetKcal());
        snapshot.setProteinG(targets.proteinG());
        snapshot.setFatG(targets.fatG());
        snapshot.setCarbsG(targets.carbsG());
        snapshot.setBasis(targets.basis());
        return snapshot;
    }
}
