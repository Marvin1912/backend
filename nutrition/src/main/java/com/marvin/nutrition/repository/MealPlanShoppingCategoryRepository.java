package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanShoppingCategoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealPlanShoppingCategoryEntity}. */
@Repository
public interface MealPlanShoppingCategoryRepository extends JpaRepository<MealPlanShoppingCategoryEntity, UUID> {

    /**
     * Returns all shopping categories belonging to the given meal plan, ordered by their display position.
     *
     * @param mealPlanId the id of the meal plan
     * @return list of shopping categories ordered by sort order ascending
     */
    List<MealPlanShoppingCategoryEntity> findAllByMealPlanIdOrderBySortOrderAsc(Long mealPlanId);
}
