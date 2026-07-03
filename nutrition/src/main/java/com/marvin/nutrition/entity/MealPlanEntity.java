package com.marvin.nutrition.entity;

import com.marvin.costs.entity.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing the singleton header row of the weekly meal-plan reference document. */
@Getter
@Setter
@Entity
@Table(name = "meal_plan", schema = "nutrition")
public class MealPlanEntity extends BasicEntity {

    /** Fixed identifier of the single meal-plan row, enforced by a DB-level CHECK constraint. */
    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "eyebrow", nullable = false, length = 500)
    private String eyebrow;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "shopping_list_title", nullable = false, length = 500)
    private String shoppingListTitle;

    @Column(name = "shopping_list_note", nullable = false, length = 500)
    private String shoppingListNote;

    @Column(name = "shopping_list_callout")
    private String shoppingListCallout;

    @Column(name = "footer_note", nullable = false)
    private String footerNote;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MealPlanEntity that = (MealPlanEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
