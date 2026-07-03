package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanSourceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealPlanSourceEntity}. */
@Repository
public interface MealPlanSourceRepository extends JpaRepository<MealPlanSourceEntity, UUID> {

    /**
     * Returns all footer sources belonging to the given meal plan, ordered by their display position.
     *
     * @param mealPlanId the id of the meal plan
     * @return list of sources ordered by sort order ascending
     */
    List<MealPlanSourceEntity> findAllByMealPlanIdOrderBySortOrderAsc(Long mealPlanId);
}
