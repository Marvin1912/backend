package com.marvin.nutrition.entity;

import com.marvin.costs.entity.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing a single food item with quantity within a {@link MealTemplateEntity}. */
@Getter
@Setter
@Entity
@Table(name = "meal_template_item", schema = "nutrition")
public class MealTemplateItemEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "meal_template_id", nullable = false)
    private UUID mealTemplateId;

    @Column(name = "food_id", nullable = false)
    private UUID foodId;

    @Column(name = "quantity_g", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityG;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MealTemplateItemEntity that = (MealTemplateItemEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
