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
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing a food item in the nutrition food catalog. */
@Getter
@Setter
@Entity
@Table(name = "food", schema = "nutrition")
public class FoodEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "kcal_per_100", nullable = false, precision = 10, scale = 2)
    private BigDecimal kcalPer100;

    @Column(name = "protein_per_100", nullable = false, precision = 10, scale = 2)
    private BigDecimal proteinPer100;

    @Column(name = "carbs_per_100", nullable = false, precision = 10, scale = 2)
    private BigDecimal carbsPer100;

    @Column(name = "fat_per_100", nullable = false, precision = 10, scale = 2)
    private BigDecimal fatPer100;

    @Column(name = "fiber_per_100", precision = 10, scale = 2)
    private BigDecimal fiberPer100;

    @Column(name = "default_serving_g", precision = 10, scale = 2)
    private BigDecimal defaultServingG;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private FoodSource source;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FoodEntity that = (FoodEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
