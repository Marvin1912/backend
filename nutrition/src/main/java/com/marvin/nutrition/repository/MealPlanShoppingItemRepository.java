package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanShoppingItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealPlanShoppingItemEntity}. */
@Repository
public interface MealPlanShoppingItemRepository extends JpaRepository<MealPlanShoppingItemEntity, UUID> {

    /**
     * Returns all items belonging to the given shopping category, ordered by their display position.
     *
     * @param mealPlanShoppingCategoryId the id of the shopping category
     * @return list of items ordered by sort order ascending
     */
    List<MealPlanShoppingItemEntity> findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(UUID mealPlanShoppingCategoryId);
}
