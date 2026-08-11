package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealEntryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealEntryEntity}. */
@Repository
public interface MealEntryRepository extends JpaRepository<MealEntryEntity, UUID> {

    /**
     * Returns all meal entries for the given date, ordered by their creation timestamp ascending.
     *
     * @param entryDate the date to query
     * @return list of meal entries for that date in creation order
     */
    List<MealEntryEntity> findByEntryDateOrderByCreationDateAsc(LocalDate entryDate);

    /**
     * Returns all meal entries within the given date range (inclusive), ordered by date and then by
     * their creation timestamp ascending.
     *
     * @param from the first date to include
     * @param to   the last date to include
     * @return list of meal entries within the range in date and creation order
     */
    List<MealEntryEntity> findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(LocalDate from, LocalDate to);

    /**
     * Returns the earliest ever recorded meal entry, ordered by entry date ascending.
     * Used to determine the start date of the entire tracked period.
     *
     * @return an Optional containing the earliest entry, or empty if no entries exist
     */
    Optional<MealEntryEntity> findTopByOrderByEntryDateAsc();
}
