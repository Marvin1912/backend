package com.marvin.nutrition.entity;

import com.marvin.costs.entity.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing a single meal log entry for a given day. */
@Getter
@Setter
@Entity
@Table(name = "meal_entry", schema = "nutrition")
public class MealEntryEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(name = "food_id")
    private UUID foodId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "food_name", length = 255)
    private String foodName;

    @Column(name = "quantity_g", precision = 10, scale = 2)
    private BigDecimal quantityG;

    @Column(name = "kcal", nullable = false, precision = 10, scale = 2)
    private BigDecimal kcal;

    @Column(name = "protein_g", nullable = false, precision = 10, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", nullable = false, precision = 10, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "fat_g", nullable = false, precision = 10, scale = 2)
    private BigDecimal fatG;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MealEntryEntity that = (MealEntryEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
