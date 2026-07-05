package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealPlanSectionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link MealPlanSectionAssembler} covering multi-section row assembly. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanSectionAssembler Tests")
class MealPlanSectionAssemblerTest {

    @Mock
    private MealPlanSectionRepository mealPlanSectionRepository;

    @Mock
    private MealPlanRowRepository mealPlanRowRepository;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @InjectMocks
    private MealPlanSectionAssembler mealPlanSectionAssembler;

    @Test
    @DisplayName("assemble loads each section's own rows and maps them via the mapper")
    void assemble_LoadsRowsPerSectionAndMaps() {
        final Long mealPlanId = 1L;
        final UUID sectionOneId = UUID.randomUUID();
        final UUID sectionTwoId = UUID.randomUUID();

        final MealPlanSectionEntity sectionOne = new MealPlanSectionEntity();
        sectionOne.setId(sectionOneId);
        sectionOne.setTitle("1 · Tagesstruktur");

        final MealPlanSectionEntity sectionTwo = new MealPlanSectionEntity();
        sectionTwo.setId(sectionTwoId);
        sectionTwo.setTitle("2 · Wochentage");

        final MealPlanRowEntity rowOneEntity = new MealPlanRowEntity();
        rowOneEntity.setId(UUID.randomUUID());
        rowOneEntity.setMealType(MealType.BREAKFAST);
        final MealPlanRowEntity rowTwoEntity = new MealPlanRowEntity();
        rowTwoEntity.setId(UUID.randomUUID());
        rowTwoEntity.setMealType(MealType.DINNER);

        final MealPlanRowDTO rowOneDTO = new MealPlanRowDTO(
                rowOneEntity.getId(), MealType.BREAKFAST, UUID.randomUUID(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));
        final MealPlanRowDTO rowTwoDTO = new MealPlanRowDTO(
                rowTwoEntity.getId(), MealType.DINNER, UUID.randomUUID(), "Hähnchenbrustfilet",
                new BigDecimal("170.00"), new BigDecimal("923.00"), new BigDecimal("73.90"),
                new BigDecimal("60.00"), new BigDecimal("20.00"));

        final MealPlanSectionDTO sectionOneDTO =
                new MealPlanSectionDTO(sectionOneId, "1 · Tagesstruktur", "note", List.of(rowOneDTO), null);
        final MealPlanSectionDTO sectionTwoDTO =
                new MealPlanSectionDTO(sectionTwoId, "2 · Wochentage", "note", List.of(rowTwoDTO), null);

        when(mealPlanSectionRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlanId))
                .thenReturn(List.of(sectionOne, sectionTwo));
        when(mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(sectionOneId))
                .thenReturn(List.of(rowOneEntity));
        when(mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(sectionTwoId))
                .thenReturn(List.of(rowTwoEntity));
        when(mealPlanMapper.toRowDTOs(List.of(rowOneEntity))).thenReturn(List.of(rowOneDTO));
        when(mealPlanMapper.toRowDTOs(List.of(rowTwoEntity))).thenReturn(List.of(rowTwoDTO));
        when(mealPlanMapper.toSectionDTO(sectionOne, List.of(rowOneDTO))).thenReturn(sectionOneDTO);
        when(mealPlanMapper.toSectionDTO(sectionTwo, List.of(rowTwoDTO))).thenReturn(sectionTwoDTO);

        final List<MealPlanSectionDTO> result = mealPlanSectionAssembler.assemble(mealPlanId);

        assertEquals(List.of(sectionOneDTO, sectionTwoDTO), result);
    }

    @Test
    @DisplayName("assemble returns an empty list when the meal plan has no sections")
    void assemble_NoSections_ReturnsEmptyList() {
        final Long mealPlanId = 1L;
        when(mealPlanSectionRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlanId)).thenReturn(List.of());

        final List<MealPlanSectionDTO> result = mealPlanSectionAssembler.assemble(mealPlanId);

        assertEquals(List.of(), result);
    }
}
