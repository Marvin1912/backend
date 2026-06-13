package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for per-day nutrition target snapshots. */
public interface DayTargetSnapshotRepository extends JpaRepository<DayTargetSnapshotEntity, LocalDate> {

    /**
     * Returns all target snapshots whose date falls within the given range (inclusive).
     *
     * @param from the first date to include
     * @param to   the last date to include
     * @return list of snapshots within the range
     */
    List<DayTargetSnapshotEntity> findByEntryDateBetween(LocalDate from, LocalDate to);
}
