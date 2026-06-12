package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for per-day nutrition target snapshots. */
public interface DayTargetSnapshotRepository extends JpaRepository<DayTargetSnapshotEntity, LocalDate> {
}
