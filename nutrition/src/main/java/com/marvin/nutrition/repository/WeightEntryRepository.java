package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.WeightEntryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for body-weight measurements. */
public interface WeightEntryRepository extends JpaRepository<WeightEntryEntity, Long> {

    /**
     * Returns the most recent weight entry ordered by entry date descending.
     *
     * @return an Optional containing the latest entry, or empty if no entries exist
     */
    Optional<WeightEntryEntity> findTopByOrderByEntryDateDesc();

    /**
     * Returns the most recent weight entry on or before the given date, ordered by entry date descending.
     * Used to determine the weight that applied as of a historical date.
     *
     * @param date the date to find the applicable weight entry for
     * @return an Optional containing the applicable entry, or empty if no entry exists on or before that date
     */
    Optional<WeightEntryEntity> findTopByEntryDateLessThanEqualOrderByEntryDateDesc(LocalDate date);

    /**
     * Returns all weight entries ordered by entry date descending.
     *
     * @return list of all weight entries, newest first
     */
    List<WeightEntryEntity> findAllByOrderByEntryDateDesc();

    /**
     * Returns the earliest weight entry with an entry date strictly after the given date, ordered
     * by entry date ascending. Used to determine the upper bound (exclusive) of the date range for
     * which a given weight entry is applicable.
     *
     * @param date the date to find the next weight entry after
     * @return an Optional containing the next entry, or empty if no entry exists after that date
     */
    Optional<WeightEntryEntity> findTopByEntryDateGreaterThanOrderByEntryDateAsc(LocalDate date);
}
