package com.marvin.nutrition.entity;

import com.marvin.costs.entity.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing the nutrition targets that applied on a specific calendar day. */
@Getter
@Setter
@Entity
@Table(name = "day_target_snapshot", schema = "nutrition")
public class DayTargetSnapshotEntity extends BasicEntity {

    @Id
    @Column(name = "entry_date", nullable = false, updatable = false)
    private LocalDate entryDate;

    @Column(name = "bmr", nullable = false)
    private int bmr;

    @Column(name = "maintenance_kcal", nullable = false)
    private int maintenanceKcal;

    @Column(name = "target_kcal", nullable = false)
    private int targetKcal;

    @Column(name = "protein_g", nullable = false)
    private int proteinG;

    @Column(name = "fat_g", nullable = false)
    private int fatG;

    @Column(name = "carbs_g", nullable = false)
    private int carbsG;

    @Column(name = "basis", nullable = false, length = 20)
    private String basis;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DayTargetSnapshotEntity that = (DayTargetSnapshotEntity) o;
        return Objects.equals(entryDate, that.entryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entryDate);
    }
}
