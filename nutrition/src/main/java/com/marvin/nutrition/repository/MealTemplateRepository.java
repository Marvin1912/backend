package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealTemplateEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link MealTemplateEntity}. */
@Repository
public interface MealTemplateRepository extends JpaRepository<MealTemplateEntity, UUID> {

    /**
     * Returns all meal templates ordered alphabetically by name.
     *
     * @return list of meal templates ordered by name ascending
     */
    List<MealTemplateEntity> findAllByOrderByNameAsc();
}
