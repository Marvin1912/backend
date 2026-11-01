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

/**
 * JPA entity marking a calendar day as excluded from nutrition evaluations (averages, ratios,
 * adherence), e.g. because meals were eaten out and could not be tracked meaningfully.
 */
@Getter
@Setter
@Entity
@Table(name = "excluded_day", schema = "nutrition")
public class ExcludedDayEntity extends BasicEntity {

    @Id
    @Column(name = "entry_date", nullable = false, updatable = false)
    private LocalDate entryDate;

    @Column(name = "reason")
    private String reason;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ExcludedDayEntity that = (ExcludedDayEntity) o;
        return Objects.equals(entryDate, that.entryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entryDate);
    }
}
