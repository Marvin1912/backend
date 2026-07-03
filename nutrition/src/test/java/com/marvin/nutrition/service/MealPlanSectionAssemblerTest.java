package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealPlanSectionRepository;
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
        rowOneEntity.setMeal("Frühstück");
        final MealPlanRowEntity rowTwoEntity = new MealPlanRowEntity();
        rowTwoEntity.setId(UUID.randomUUID());
        rowTwoEntity.setMeal("Abendessen");

        final MealPlanRowDTO rowOneDTO = new MealPlanRowDTO("Frühstück", "details", "qty", "519", "28,0 g");
        final MealPlanRowDTO rowTwoDTO = new MealPlanRowDTO("Abendessen", "details", "qty", "923", "73,9 g");

        final MealPlanSectionDTO sectionOneDTO =
                new MealPlanSectionDTO("1 · Tagesstruktur", "note", List.of(rowOneDTO), null, null);
        final MealPlanSectionDTO sectionTwoDTO =
                new MealPlanSectionDTO("2 · Wochentage", "note", List.of(rowTwoDTO), null, null);

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
