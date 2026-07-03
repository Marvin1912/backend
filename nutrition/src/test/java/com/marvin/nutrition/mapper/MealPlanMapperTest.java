package com.marvin.nutrition.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MealPlanMapper}, focused on the nullable {@code totals} branch of {@code toSectionDTO}. */
@DisplayName("MealPlanMapper Tests")
class MealPlanMapperTest {

    private final MealPlanMapper mealPlanMapper = new MealPlanMapperImpl();

    @Test
    @DisplayName("toSectionDTO returns null totals when the section has no totals label")
    void toSectionDTO_NoTotalsLabel_ReturnsNullTotals() {
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setTitle("1 · Tagesstruktur (täglich gleich)");
        section.setNote("note");
        section.setCallout("callout");

        final MealPlanSectionDTO dto = mealPlanMapper.toSectionDTO(section, List.of());

        assertEquals("1 · Tagesstruktur (täglich gleich)", dto.title());
        assertNull(dto.totals());
    }

    @Test
    @DisplayName("toSectionDTO builds totals when the section has a totals label")
    void toSectionDTO_WithTotalsLabel_ReturnsTotals() {
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setTitle("2 · Wochentage (Montag – Donnerstag)");
        section.setNote("note");
        section.setTotalsLabel("Tagesgesamt");
        section.setTotalsKcal("2.407 kcal");
        section.setTotalsProtein("182,2 g");

        final MealPlanRowDTO row = new MealPlanRowDTO("Frühstück", "details", "qty", "519", "28,0 g");
        final MealPlanSectionDTO dto = mealPlanMapper.toSectionDTO(section, List.of(row));

        assertEquals(List.of(row), dto.rows());
        assertEquals("Tagesgesamt", dto.totals().label());
        assertEquals("2.407 kcal", dto.totals().kcal());
        assertEquals("182,2 g", dto.totals().protein());
    }
}
