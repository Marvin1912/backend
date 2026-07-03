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

/** JPA entity representing a single item on the shopping list within a {@link MealPlanShoppingCategoryEntity}. */
@Getter
@Setter
@Entity
@Table(name = "meal_plan_shopping_item", schema = "nutrition")
public class MealPlanShoppingItemEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "meal_plan_shopping_category_id", nullable = false)
    private UUID mealPlanShoppingCategoryId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "brand", length = 500)
    private String brand;

    @Column(name = "badge", length = 10)
    private String badge;

    @Column(name = "badge_text", length = 500)
    private String badgeText;

    @Column(name = "qty", nullable = false, length = 255)
    private String qty;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MealPlanShoppingItemEntity that = (MealPlanShoppingItemEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
