package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.MealPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for the single-row meal-plan header. */
@Repository
public interface MealPlanRepository extends JpaRepository<MealPlanEntity, Long> {
}
