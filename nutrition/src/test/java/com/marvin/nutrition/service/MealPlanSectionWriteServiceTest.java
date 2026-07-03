package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealPlanSectionRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link MealPlanSectionWriteService} covering section and row content updates. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanSectionWriteService Tests")
class MealPlanSectionWriteServiceTest {

    @Mock
    private MealPlanSectionRepository mealPlanSectionRepository;

    @Mock
    private MealPlanRowRepository mealPlanRowRepository;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @InjectMocks
    private MealPlanSectionWriteService mealPlanSectionWriteService;

    // -----------------------------------------------------------------------
    // updateSection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateSection applies non-null fields, saves and returns the section with its rows")
    void updateSection_AppliesNonNullFields_ReturnsUpdatedSectionWithRows() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);
        section.setTitle("old title");
        section.setNote("old note");

        final MealPlanRowEntity rowEntity = new MealPlanRowEntity();
        rowEntity.setId(UUID.randomUUID());

        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(rowEntity.getId(), "Frühstück", "details", "qty", "519", "28,0 g");
        final MealPlanSectionDTO sectionDTO =
                new MealPlanSectionDTO(sectionId, "new title", "old note", List.of(rowDTO), null, null);

        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("new title", null, null);

        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(mealPlanSectionRepository.save(section)).thenReturn(section);
        when(mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(sectionId))
                .thenReturn(List.of(rowEntity));
        when(mealPlanMapper.toRowDTOs(List.of(rowEntity))).thenReturn(List.of(rowDTO));
        when(mealPlanMapper.toSectionDTO(section, List.of(rowDTO))).thenReturn(sectionDTO);

        final MealPlanSectionDTO result = mealPlanSectionWriteService.updateSection(sectionId, req);

        assertEquals("new title", section.getTitle());
        assertEquals("old note", section.getNote());
        assertEquals(sectionDTO, result);
    }

    @Test
    @DisplayName("updateSection throws NoSuchElementException when the section does not exist")
    void updateSection_NotFound_ThrowsNoSuchElementException() {
        final UUID sectionId = UUID.randomUUID();
        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.empty());

        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("title", null, null);

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.updateSection(sectionId, req));
    }

    // -----------------------------------------------------------------------
    // updateRow
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateRow applies non-null fields, saves and returns the updated row")
    void updateRow_AppliesNonNullFields_ReturnsUpdatedRow() {
        final UUID rowId = UUID.randomUUID();
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setId(rowId);
        row.setMeal("old meal");
        row.setKcal("500");

        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(rowId, "new meal", "details", "qty", "500", "28,0 g");

        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest("new meal", null, null, null, null);

        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.of(row));
        when(mealPlanRowRepository.save(row)).thenReturn(row);
        when(mealPlanMapper.toRowDTO(row)).thenReturn(rowDTO);

        final MealPlanRowDTO result = mealPlanSectionWriteService.updateRow(rowId, req);

        assertEquals("new meal", row.getMeal());
        assertEquals("500", row.getKcal());
        assertEquals(rowDTO, result);
    }

    @Test
    @DisplayName("updateRow throws NoSuchElementException when the row does not exist")
    void updateRow_NotFound_ThrowsNoSuchElementException() {
        final UUID rowId = UUID.randomUUID();
        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.empty());

        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest("meal", null, null, null, null);

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.updateRow(rowId, req));
    }
}
