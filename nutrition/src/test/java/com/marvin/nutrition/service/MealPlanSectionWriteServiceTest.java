package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealPlanRowRequest;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealPlanSectionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link MealPlanSectionWriteService} covering section updates and food-backed row
 * create/update/delete (issue #225 rewrite), including the sort-order collision fix and batched
 * food lookups found in code review of PR #226.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanSectionWriteService Tests")
class MealPlanSectionWriteServiceTest {

    @Mock
    private MealPlanSectionRepository mealPlanSectionRepository;

    @Mock
    private MealPlanRowRepository mealPlanRowRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @InjectMocks
    private MealPlanSectionWriteService mealPlanSectionWriteService;

    private FoodEntity food(BigDecimal kcal, BigDecimal protein, BigDecimal carbs, BigDecimal fat) {
        final FoodEntity food = new FoodEntity();
        food.setId(UUID.randomUUID());
        food.setName("Haferflocken");
        food.setKcalPer100(kcal);
        food.setProteinPer100(protein);
        food.setCarbsPer100(carbs);
        food.setFatPer100(fat);
        return food;
    }

    private MealPlanRowEntity rowWithSortOrder(int sortOrder) {
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setId(UUID.randomUUID());
        row.setSortOrder(sortOrder);
        return row;
    }

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

        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(
                rowEntity.getId(), MealType.BREAKFAST, UUID.randomUUID(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));
        final MealPlanSectionDTO sectionDTO =
                new MealPlanSectionDTO(sectionId, "new title", "old note", List.of(rowDTO), null);

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
    // addRow
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addRow derives macros from the food's per-100g values and the given quantity")
    void addRow_DerivesMacrosFromFood() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);

        final FoodEntity food = food(
                new BigDecimal("577.00"), new BigDecimal("31.00"), new BigDecimal("66.00"), new BigDecimal("11.00"));
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, food.getId(), new BigDecimal("90.00"));

        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
        when(mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(sectionId))
                .thenReturn(Optional.empty());
        when(mealPlanRowRepository.save(any(MealPlanRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealPlanMapper.toRowDTO(any(MealPlanRowEntity.class))).thenAnswer(inv -> {
            final MealPlanRowEntity saved = inv.getArgument(0);
            return new MealPlanRowDTO(saved.getId(), saved.getMealType(), saved.getFoodId(), saved.getFoodName(),
                    saved.getQuantityG(), saved.getKcal(), saved.getProteinG(), saved.getCarbsG(), saved.getFatG());
        });

        final MealPlanRowDTO result = mealPlanSectionWriteService.addRow(sectionId, req);

        assertEquals(MealType.BREAKFAST, result.mealType());
        assertEquals(food.getId(), result.foodId());
        assertEquals("Haferflocken", result.foodName());
        assertEquals(new BigDecimal("519.30"), result.kcal());
        assertEquals(new BigDecimal("27.90"), result.proteinG());
        assertEquals(new BigDecimal("59.40"), result.carbsG());
        assertEquals(new BigDecimal("9.90"), result.fatG());

        final ArgumentCaptor<MealPlanRowEntity> captor = ArgumentCaptor.forClass(MealPlanRowEntity.class);
        verify(mealPlanRowRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getSortOrder());
        assertEquals(sectionId, captor.getValue().getMealPlanSectionId());
    }

    @Test
    @DisplayName("addRow assigns the next sort order as max(existing) + 1")
    void addRow_AssignsNextSortOrder() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);

        final FoodEntity food = food(
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.SNACK, food.getId(), new BigDecimal("100.00"));

        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
        when(mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(sectionId))
                .thenReturn(Optional.of(rowWithSortOrder(1)));
        when(mealPlanRowRepository.save(any(MealPlanRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealPlanMapper.toRowDTO(any(MealPlanRowEntity.class))).thenReturn(null);

        mealPlanSectionWriteService.addRow(sectionId, req);

        final ArgumentCaptor<MealPlanRowEntity> captor = ArgumentCaptor.forClass(MealPlanRowEntity.class);
        verify(mealPlanRowRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getSortOrder());
    }

    @Test
    @DisplayName("addRow after a middle row was deleted assigns a sort order past the remaining max, not a colliding count-based value")
    void addRow_AfterMiddleRowDeleted_DoesNotCollideWithRemainingSortOrder() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);

        final FoodEntity food = food(
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.SNACK, food.getId(), new BigDecimal("100.00"));

        // Section originally had rows at sort_order 0, 1, 2; the row at 1 was deleted, leaving 0 and 2.
        // A count-based approach would compute size()=2 here and collide with the remaining row at 2.
        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
        when(mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(sectionId))
                .thenReturn(Optional.of(rowWithSortOrder(2)));
        when(mealPlanRowRepository.save(any(MealPlanRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealPlanMapper.toRowDTO(any(MealPlanRowEntity.class))).thenReturn(null);

        mealPlanSectionWriteService.addRow(sectionId, req);

        final ArgumentCaptor<MealPlanRowEntity> captor = ArgumentCaptor.forClass(MealPlanRowEntity.class);
        verify(mealPlanRowRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getSortOrder());
    }

    @Test
    @DisplayName("addRow throws NoSuchElementException when the section does not exist")
    void addRow_SectionNotFound_ThrowsNoSuchElementException() {
        final UUID sectionId = UUID.randomUUID();
        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.empty());

        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.addRow(sectionId, req));
        verify(foodRepository, never()).findById(any());
    }

    @Test
    @DisplayName("addRow throws NoSuchElementException when the referenced food does not exist")
    void addRow_FoodNotFound_ThrowsNoSuchElementException() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);
        final UUID foodId = UUID.randomUUID();

        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(foodRepository.findById(foodId)).thenReturn(Optional.empty());

        final CreateMealPlanRowRequest req = new CreateMealPlanRowRequest(MealType.BREAKFAST, foodId, new BigDecimal("90.00"));

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.addRow(sectionId, req));
        verify(mealPlanRowRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // addRows (batch)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addRows creates one row per request with incrementing sort order")
    void addRows_CreatesAllRowsWithIncrementingSortOrder() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);

        final FoodEntity foodOne = food(
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));
        final FoodEntity foodTwo = food(
                new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("20.00"), new BigDecimal("20.00"));

        final List<CreateMealPlanRowRequest> requests = List.of(
                new CreateMealPlanRowRequest(MealType.BREAKFAST, foodOne.getId(), new BigDecimal("100.00")),
                new CreateMealPlanRowRequest(MealType.LUNCH, foodTwo.getId(), new BigDecimal("100.00")));

        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(sectionId))
                .thenReturn(Optional.empty());
        when(foodRepository.findAllById(any())).thenReturn(List.of(foodOne, foodTwo));
        when(mealPlanRowRepository.save(any(MealPlanRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealPlanMapper.toRowDTO(any(MealPlanRowEntity.class))).thenReturn(null);

        mealPlanSectionWriteService.addRows(sectionId, requests);

        final ArgumentCaptor<MealPlanRowEntity> captor = ArgumentCaptor.forClass(MealPlanRowEntity.class);
        verify(mealPlanRowRepository, times(2)).save(captor.capture());
        assertEquals(List.of(0, 1), captor.getAllValues().stream().map(MealPlanRowEntity::getSortOrder).toList());
    }

    @Test
    @DisplayName("addRows looks up all referenced foods in a single batch call, not one findById per row")
    void addRows_BatchesFoodLookupViaFindAllById() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);

        final FoodEntity foodOne = food(
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));
        final FoodEntity foodTwo = food(
                new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("20.00"), new BigDecimal("20.00"));

        final List<CreateMealPlanRowRequest> requests = List.of(
                new CreateMealPlanRowRequest(MealType.BREAKFAST, foodOne.getId(), new BigDecimal("100.00")),
                new CreateMealPlanRowRequest(MealType.LUNCH, foodTwo.getId(), new BigDecimal("100.00")));

        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(sectionId))
                .thenReturn(Optional.empty());
        when(foodRepository.findAllById(any())).thenReturn(List.of(foodOne, foodTwo));
        when(mealPlanRowRepository.save(any(MealPlanRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealPlanMapper.toRowDTO(any(MealPlanRowEntity.class))).thenReturn(null);

        mealPlanSectionWriteService.addRows(sectionId, requests);

        verify(foodRepository, times(1)).findAllById(any());
        verify(foodRepository, never()).findById(any());
    }

    @Test
    @DisplayName("addRows throws NoSuchElementException and saves nothing further when a later food is unknown")
    void addRows_UnknownFoodPartwayThrough_ThrowsAndSavesNothingFurther() {
        final UUID sectionId = UUID.randomUUID();
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setId(sectionId);

        final FoodEntity foodOne = food(
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));
        final UUID unknownFoodId = UUID.randomUUID();

        final List<CreateMealPlanRowRequest> requests = List.of(
                new CreateMealPlanRowRequest(MealType.BREAKFAST, foodOne.getId(), new BigDecimal("100.00")),
                new CreateMealPlanRowRequest(MealType.LUNCH, unknownFoodId, new BigDecimal("100.00")));

        when(mealPlanSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(sectionId))
                .thenReturn(Optional.empty());
        // unknownFoodId is simply absent from the batch result, as findAllById would return for it.
        when(foodRepository.findAllById(any())).thenReturn(List.of(foodOne));
        when(mealPlanRowRepository.save(any(MealPlanRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealPlanMapper.toRowDTO(any(MealPlanRowEntity.class))).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.addRows(sectionId, requests));

        // The service itself does not roll back (that's @Transactional's job at the DB level);
        // it must, however, stop processing at the point of failure rather than continuing past it.
        verify(mealPlanRowRepository, times(1)).save(any(MealPlanRowEntity.class));
    }

    // -----------------------------------------------------------------------
    // updateRow
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateRow re-snapshots macros from the referenced food and the new quantity")
    void updateRow_ResnapshotsMacros() {
        final UUID rowId = UUID.randomUUID();
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setId(rowId);
        row.setMealType(MealType.BREAKFAST);

        final FoodEntity food = food(
                new BigDecimal("577.00"), new BigDecimal("31.00"), new BigDecimal("66.00"), new BigDecimal("11.00"));
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.DINNER, food.getId(), new BigDecimal("200.00"));

        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.of(row));
        when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
        when(mealPlanRowRepository.save(row)).thenReturn(row);
        when(mealPlanMapper.toRowDTO(row)).thenAnswer(inv -> new MealPlanRowDTO(
                row.getId(), row.getMealType(), row.getFoodId(), row.getFoodName(),
                row.getQuantityG(), row.getKcal(), row.getProteinG(), row.getCarbsG(), row.getFatG()));

        final MealPlanRowDTO result = mealPlanSectionWriteService.updateRow(rowId, req);

        assertEquals(MealType.DINNER, result.mealType());
        assertEquals(food.getId(), result.foodId());
        assertEquals("Haferflocken", result.foodName());
        assertEquals(new BigDecimal("200.00"), result.quantityG());
        assertEquals(new BigDecimal("1154.00"), result.kcal());
        assertEquals(new BigDecimal("62.00"), result.proteinG());
        assertEquals(new BigDecimal("132.00"), result.carbsG());
        assertEquals(new BigDecimal("22.00"), result.fatG());
    }

    @Test
    @DisplayName("updateRow leaves mealType unchanged when the request's mealType is null")
    void updateRow_NullMealType_LeavesMealTypeUnchanged() {
        final UUID rowId = UUID.randomUUID();
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setId(rowId);
        row.setMealType(MealType.BREAKFAST);

        final FoodEntity food = food(
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(null, food.getId(), new BigDecimal("100.00"));

        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.of(row));
        when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
        when(mealPlanRowRepository.save(row)).thenReturn(row);
        when(mealPlanMapper.toRowDTO(row)).thenReturn(null);

        mealPlanSectionWriteService.updateRow(rowId, req);

        assertEquals(MealType.BREAKFAST, row.getMealType());
    }

    @Test
    @DisplayName("updateRow throws NoSuchElementException when the row does not exist")
    void updateRow_RowNotFound_ThrowsNoSuchElementException() {
        final UUID rowId = UUID.randomUUID();
        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.empty());

        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, UUID.randomUUID(), new BigDecimal("100.00"));

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.updateRow(rowId, req));
    }

    @Test
    @DisplayName("updateRow throws NoSuchElementException when the referenced food does not exist")
    void updateRow_FoodNotFound_ThrowsNoSuchElementException() {
        final UUID rowId = UUID.randomUUID();
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setId(rowId);
        final UUID foodId = UUID.randomUUID();

        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.of(row));
        when(foodRepository.findById(foodId)).thenReturn(Optional.empty());

        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, foodId, new BigDecimal("100.00"));

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.updateRow(rowId, req));
        verify(mealPlanRowRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // deleteRow
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteRow deletes the found row entity")
    void deleteRow_DeletesFoundEntity() {
        final UUID rowId = UUID.randomUUID();
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setId(rowId);

        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.of(row));

        mealPlanSectionWriteService.deleteRow(rowId);

        verify(mealPlanRowRepository).delete(row);
    }

    @Test
    @DisplayName("deleteRow throws NoSuchElementException when the row does not exist")
    void deleteRow_NotFound_ThrowsNoSuchElementException() {
        final UUID rowId = UUID.randomUUID();
        when(mealPlanRowRepository.findById(rowId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> mealPlanSectionWriteService.deleteRow(rowId));
    }
}
