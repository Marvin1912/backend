package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanRowEntity;
import java.util.List;
import java.util.Optional;
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

    /**
     * Returns the row with the highest sort order within the given section, if any.
     * Used to derive the next sort order for a newly created row as {@code max + 1}, rather than a
     * count, so that a row deleted from the middle of a section can never cause a new row to collide
     * with a sort order still in use by a remaining row.
     *
     * @param mealPlanSectionId the id of the meal plan section
     * @return the row with the highest sort order, or empty if the section has no rows
     */
    Optional<MealPlanRowEntity> findFirstByMealPlanSectionIdOrderBySortOrderDesc(UUID mealPlanSectionId);

    /**
     * Counts how many rows reference the given food, used to guard against deleting a still-referenced food.
     *
     * @param foodId the id of the food catalog entry
     * @return the number of rows referencing the food
     */
    long countByFoodId(UUID foodId);
}
