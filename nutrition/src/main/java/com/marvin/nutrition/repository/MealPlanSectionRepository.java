package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanSectionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealPlanSectionEntity}. */
@Repository
public interface MealPlanSectionRepository extends JpaRepository<MealPlanSectionEntity, UUID> {

    /**
     * Returns all sections belonging to the given meal plan, ordered by their display position.
     *
     * @param mealPlanId the id of the meal plan
     * @return list of sections ordered by sort order ascending
     */
    List<MealPlanSectionEntity> findAllByMealPlanIdOrderBySortOrderAsc(Long mealPlanId);
}
