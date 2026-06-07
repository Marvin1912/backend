package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.WeightEntryEntity;
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
     * Returns all weight entries ordered by entry date descending.
     *
     * @return list of all weight entries, newest first
     */
    List<WeightEntryEntity> findAllByOrderByEntryDateDesc();
}
