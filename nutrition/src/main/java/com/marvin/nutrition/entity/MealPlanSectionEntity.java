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

/** JPA entity representing a single section of the meal plan, e.g. a daily structure or a group of weekdays. */
@Getter
@Setter
@Entity
@Table(name = "meal_plan_section", schema = "nutrition")
public class MealPlanSectionEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "meal_plan_id", nullable = false)
    private Long mealPlanId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "note", nullable = false, length = 500)
    private String note;

    @Column(name = "callout")
    private String callout;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MealPlanSectionEntity that = (MealPlanSectionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
