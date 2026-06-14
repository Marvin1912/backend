package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealTemplateItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealTemplateItemEntity}. */
@Repository
public interface MealTemplateItemRepository extends JpaRepository<MealTemplateItemEntity, UUID> {

    /**
     * Returns all items belonging to the given meal template.
     *
     * @param mealTemplateId the id of the meal template
     * @return list of items belonging to the meal template
     */
    List<MealTemplateItemEntity> findByMealTemplateId(UUID mealTemplateId);

    /**
     * Deletes all items belonging to the given meal template.
     *
     * @param mealTemplateId the id of the meal template
     */
    void deleteByMealTemplateId(UUID mealTemplateId);
}
