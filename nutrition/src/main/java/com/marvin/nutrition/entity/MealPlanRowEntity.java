package com.marvin.nutrition.entity;

import com.marvin.costs.entity.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing a single meal row within a {@link MealPlanSectionEntity}. */
@Getter
@Setter
@Entity
@Table(name = "meal_plan_row", schema = "nutrition")
public class MealPlanRowEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "meal_plan_section_id", nullable = false)
    private UUID mealPlanSectionId;

    @Column(name = "meal", nullable = false, length = 255)
    private String meal;

    @Column(name = "details", nullable = false)
    private String details;

    @Column(name = "qty", nullable = false, length = 255)
    private String qty;

    @Column(name = "kcal", nullable = false, length = 255)
    private String kcal;

    @Column(name = "protein", nullable = false, length = 255)
    private String protein;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MealPlanRowEntity that = (MealPlanRowEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
