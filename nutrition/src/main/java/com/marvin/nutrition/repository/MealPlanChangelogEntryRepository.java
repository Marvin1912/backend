package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanChangelogEntryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealPlanChangelogEntryEntity}. */
@Repository
public interface MealPlanChangelogEntryRepository extends JpaRepository<MealPlanChangelogEntryEntity, UUID> {

    /**
     * Returns all changelog entries belonging to the given meal plan, ordered by their display position.
     *
     * @param mealPlanId the id of the meal plan
     * @return list of changelog entries ordered by sort order ascending
     */
    List<MealPlanChangelogEntryEntity> findAllByMealPlanIdOrderBySortOrderAsc(Long mealPlanId);
}
