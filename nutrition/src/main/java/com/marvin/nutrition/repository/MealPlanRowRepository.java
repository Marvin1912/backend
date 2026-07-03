package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanRowEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealPlanRowEntity}. */
@Repository
public interface MealPlanRowRepository extends JpaRepository<MealPlanRowEntity, UUID> {

    /**
     * Returns all rows belonging to the given meal plan section, ordered by their display position.
     *
     * @param mealPlanSectionId the id of the meal plan section
     * @return list of rows ordered by sort order ascending
     */
    List<MealPlanRowEntity> findAllByMealPlanSectionIdOrderBySortOrderAsc(UUID mealPlanSectionId);
}
