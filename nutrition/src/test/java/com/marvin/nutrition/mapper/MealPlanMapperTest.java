package com.marvin.nutrition.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.entity.MealType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MealPlanMapper}, covering the food-backed row shape and section assembly
 * now that stats/changelog/shopping-list/totals no longer exist (issue #225 rewrite).
 */
@DisplayName("MealPlanMapper Tests")
class MealPlanMapperTest {

    private final MealPlanMapper mealPlanMapper = new MealPlanMapperImpl();

    @Test
    @DisplayName("toRowDTO maps all food-backed fields, ignoring the section-id foreign key")
    void toRowDTO_MapsAllFields() {
        final UUID rowId = UUID.randomUUID();
        final UUID foodId = UUID.randomUUID();
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setId(rowId);
        row.setMealPlanSectionId(UUID.randomUUID());
        row.setMealType(MealType.BREAKFAST);
        row.setFoodId(foodId);
        row.setFoodName("Haferflocken");
        row.setQuantityG(new BigDecimal("90.00"));
        row.setKcal(new BigDecimal("519.00"));
        row.setProteinG(new BigDecimal("28.00"));
        row.setCarbsG(new BigDecimal("60.00"));
        row.setFatG(new BigDecimal("10.00"));

        final MealPlanRowDTO dto = mealPlanMapper.toRowDTO(row);

        assertEquals(rowId, dto.id());
        assertEquals(MealType.BREAKFAST, dto.mealType());
        assertEquals(foodId, dto.foodId());
        assertEquals("Haferflocken", dto.foodName());
        assertEquals(new BigDecimal("90.00"), dto.quantityG());
        assertEquals(new BigDecimal("519.00"), dto.kcal());
        assertEquals(new BigDecimal("28.00"), dto.proteinG());
        assertEquals(new BigDecimal("60.00"), dto.carbsG());
        assertEquals(new BigDecimal("10.00"), dto.fatG());
    }

    @Test
    @DisplayName("toSectionDTO carries over the section's id, title, note, callout and given rows")
    void toSectionDTO_MapsFieldsAndRows() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);
        section.setTitle("1 · Tagesstruktur (täglich gleich)");
        section.setNote("note");
        section.setCallout("callout");

        final MealPlanRowDTO row = new MealPlanRowDTO(
                UUID.randomUUID(), MealType.BREAKFAST, UUID.randomUUID(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));

        final MealPlanSectionDTO dto = mealPlanMapper.toSectionDTO(section, List.of(row));

        assertEquals(sectionId, dto.id());
        assertEquals("1 · Tagesstruktur (täglich gleich)", dto.title());
        assertEquals("note", dto.note());
        assertEquals("callout", dto.callout());
        assertEquals(List.of(row), dto.rows());
    }

    @Test
    @DisplayName("toSectionDTO returns a null callout when the section has none")
    void toSectionDTO_NullCallout_ReturnsNullCallout() {
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(UUID.randomUUID());
        section.setTitle("title");
        section.setNote("note");

        final MealPlanSectionDTO dto = mealPlanMapper.toSectionDTO(section, List.of());

        assertEquals(null, dto.callout());
    }
}
