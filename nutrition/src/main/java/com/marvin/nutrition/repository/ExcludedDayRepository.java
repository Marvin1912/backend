package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.ExcludedDayEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for days excluded from nutrition evaluations. */
public interface ExcludedDayRepository extends JpaRepository<ExcludedDayEntity, LocalDate> {

    /**
     * Returns all excluded days whose date falls within the given range (inclusive), ascending by date.
     *
     * @param from the first date to include
     * @param to   the last date to include
     * @return list of excluded days within the range
     */
    List<ExcludedDayEntity> findByEntryDateBetweenOrderByEntryDateAsc(LocalDate from, LocalDate to);
}
