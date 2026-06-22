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

/** JPA entity representing a single sport/exercise activity log entry for a given day. */
@Getter
@Setter
@Entity
@Table(name = "sport_activity", schema = "nutrition")
public class SportActivityEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private SportActivityType activityType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "kcal_burned", nullable = false, precision = 10, scale = 2)
    private BigDecimal kcalBurned;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SportActivityEntity that = (SportActivityEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
