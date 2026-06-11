package com.marvin.nutrition.entity;

import com.marvin.costs.entity.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing the user's nutrition profile. There is at most one row. */
@Getter
@Setter
@Entity
@Table(name = "profile", schema = "nutrition")
public class ProfileEntity extends BasicEntity {

    /** Fixed identifier of the single nutrition profile row, enforced by a DB-level CHECK constraint. */
    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    private Sex sex;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "height_cm", nullable = false, precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 20)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal", nullable = false, length = 10)
    private Goal goal;

    @Column(name = "protein_per_kg", nullable = false, precision = 4, scale = 2)
    private BigDecimal proteinPerKg;

    @Column(name = "fat_pct", nullable = false, precision = 4, scale = 2)
    private BigDecimal fatPct;

    @Column(name = "basal_kcal")
    private Integer basalKcal;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ProfileEntity that = (ProfileEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
