package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanStatEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealPlanStatEntity}. */
@Repository
public interface MealPlanStatRepository extends JpaRepository<MealPlanStatEntity, UUID> {

    /**
     * Returns all stats belonging to the given meal plan, ordered by their display position.
     *
     * @param mealPlanId the id of the meal plan
     * @return list of stats ordered by sort order ascending
     */
    List<MealPlanStatEntity> findAllByMealPlanIdOrderBySortOrderAsc(Long mealPlanId);
}
