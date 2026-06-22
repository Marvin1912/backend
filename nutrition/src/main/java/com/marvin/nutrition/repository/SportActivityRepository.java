package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.SportActivityEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link SportActivityEntity}. */
@Repository
public interface SportActivityRepository extends JpaRepository<SportActivityEntity, UUID> {

    /**
     * Returns all sport activities for the given date, ordered by their creation timestamp ascending.
     *
     * @param entryDate the date to query
     * @return list of sport activities for that date in creation order
     */
    List<SportActivityEntity> findByEntryDateOrderByCreationDateAsc(LocalDate entryDate);

    /**
     * Returns all sport activities within the given date range (inclusive), ordered by date and then by
     * their creation timestamp ascending.
     *
     * @param from the first date to include
     * @param to   the last date to include
     * @return list of sport activities within the range in date and creation order
     */
    List<SportActivityEntity> findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(LocalDate from, LocalDate to);
}
