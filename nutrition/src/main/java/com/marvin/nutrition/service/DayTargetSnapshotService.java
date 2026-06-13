package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.repository.DayTargetSnapshotRepository;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.time.LocalDate;
import java.util.List;
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
    private final WeightEntryRepository weightEntryRepository;

    /**
     * Creates a new DayTargetSnapshotService.
     *
     * @param dayTargetSnapshotRepository JPA repository for per-day nutrition target snapshots
     * @param nutritionTargetService      service for computing daily nutrition targets
     * @param weightEntryRepository       JPA repository for weight entries
     */
    public DayTargetSnapshotService(
            DayTargetSnapshotRepository dayTargetSnapshotRepository,
            NutritionTargetService nutritionTargetService,
            WeightEntryRepository weightEntryRepository) {
        this.dayTargetSnapshotRepository = dayTargetSnapshotRepository;
        this.nutritionTargetService = nutritionTargetService;
        this.weightEntryRepository = weightEntryRepository;
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
     * <p><strong>Transaction boundary trade-off:</strong> because this runs in {@code REQUIRES_NEW},
     * it is a separate transaction from any caller (e.g. {@code MealEntryWriteService.createEntry}).
     * This is intentional: it prevents a snapshot-insert race from poisoning or rolling back the
     * caller's transaction on Postgres. The downside is that this snapshot transaction can commit
     * even if the caller's outer transaction later fails, leaving a snapshot for a date that has
     * no corresponding meal entry. Such an orphan snapshot is harmless — {@code getDay} tolerates
     * a snapshot existing for a date with no entries.</p>
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
        // Race window: if two concurrent ensureSnapshot(date) calls reach here for the same new
        // date, both findById checks above returned empty before either insert happened. Both
        // saveAndFlush calls then merge() a transient entity with no row found for its id, so both
        // attempt an INSERT at flush time. Only one INSERT can succeed; the other collides with the
        // unique constraint on entry_date and raises DataIntegrityViolationException, which we treat
        // as success below.
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
    @Transactional
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
     * Recomputes and overwrites all existing day-target snapshots whose date falls within the range
     * affected by a weight entry change starting at {@code fromDate}.
     *
     * <p>The affected range is {@code [fromDate, nextWeightEntryDate)} if a later weight entry
     * exists, or {@code [fromDate, +inf)} otherwise. For each existing snapshot in that range, the
     * targets are recomputed via {@link NutritionTargetService#getTargetsSync(LocalDate)} and saved.
     * Snapshots for dates where targets cannot currently be computed (e.g. no profile data) are left
     * untouched. Dates without an existing snapshot are not created.</p>
     *
     * @param fromDate the first date (inclusive) of the affected range
     */
    @Transactional
    public void refreshSnapshotsFrom(LocalDate fromDate) {
        final List<DayTargetSnapshotEntity> snapshots = weightEntryRepository
                .findTopByEntryDateGreaterThanOrderByEntryDateAsc(fromDate)
                .map(WeightEntryEntity::getEntryDate)
                .map(nextWeightDate -> dayTargetSnapshotRepository.findByEntryDateBetween(fromDate, nextWeightDate.minusDays(1)))
                .orElseGet(() -> dayTargetSnapshotRepository.findByEntryDateGreaterThanEqual(fromDate));

        snapshots.forEach(this::refreshSnapshot);
    }

    /**
     * Recomputes and overwrites every existing day-target snapshot.
     *
     * <p>Snapshots for dates where targets cannot currently be computed (e.g. no profile data) are
     * left untouched.</p>
     */
    @Transactional
    public void refreshAllSnapshots() {
        dayTargetSnapshotRepository.findAll().forEach(this::refreshSnapshot);
    }

    /**
     * Recomputes the given existing snapshot's targets and saves it, unless targets cannot
     * currently be computed for its date, in which case it is left untouched.
     *
     * @param snapshot the existing snapshot entity to refresh
     */
    private void refreshSnapshot(DayTargetSnapshotEntity snapshot) {
        final LocalDate date = snapshot.getEntryDate();
        final TargetsDTO targets;
        try {
            targets = nutritionTargetService.getTargetsSync(date);
        } catch (TargetCalculationException e) {
            return;
        }
        dayTargetSnapshotRepository.save(toSnapshot(snapshot, date, targets));
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
