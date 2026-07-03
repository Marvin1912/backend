package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.entity.MealPlanShoppingCategoryEntity;
import com.marvin.nutrition.entity.MealPlanShoppingItemEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanShoppingCategoryRepository;
import com.marvin.nutrition.repository.MealPlanShoppingItemRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link MealPlanShoppingListAssembler} covering multi-category item assembly. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanShoppingListAssembler Tests")
class MealPlanShoppingListAssemblerTest {

    @Mock
    private MealPlanShoppingCategoryRepository mealPlanShoppingCategoryRepository;

    @Mock
    private MealPlanShoppingItemRepository mealPlanShoppingItemRepository;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @InjectMocks
    private MealPlanShoppingListAssembler mealPlanShoppingListAssembler;

    @Test
    @DisplayName("assemble loads each category's own items and maps them via the mapper")
    void assemble_LoadsItemsPerCategoryAndMaps() {
        final Long mealPlanId = 1L;
        final UUID categoryOneId = UUID.randomUUID();
        final UUID categoryTwoId = UUID.randomUUID();

        final MealPlanShoppingCategoryEntity categoryOne = new MealPlanShoppingCategoryEntity();
        categoryOne.setId(categoryOneId);
        categoryOne.setTitle("Fleisch & Fisch");

        final MealPlanShoppingCategoryEntity categoryTwo = new MealPlanShoppingCategoryEntity();
        categoryTwo.setId(categoryTwoId);
        categoryTwo.setTitle("Milchprodukte & Eier");

        final MealPlanShoppingItemEntity itemOneEntity = new MealPlanShoppingItemEntity();
        itemOneEntity.setId(UUID.randomUUID());
        itemOneEntity.setName("Hähnchenbrustfilet");
        final MealPlanShoppingItemEntity itemTwoEntity = new MealPlanShoppingItemEntity();
        itemTwoEntity.setId(UUID.randomUUID());
        itemTwoEntity.setName("Magerquark");

        final MealPlanShoppingItemDTO itemOneDTO =
                new MealPlanShoppingItemDTO("Hähnchenbrustfilet", "frisch, Kühltheke", "warn", "nur 1.200 g verfügbar", "1.200 g");
        final MealPlanShoppingItemDTO itemTwoDTO =
                new MealPlanShoppingItemDTO("Magerquark", "Milbona", null, null, "2.100 g");

        final MealPlanShoppingCategoryDTO categoryOneDTO =
                new MealPlanShoppingCategoryDTO("Fleisch & Fisch", List.of(itemOneDTO));
        final MealPlanShoppingCategoryDTO categoryTwoDTO =
                new MealPlanShoppingCategoryDTO("Milchprodukte & Eier", List.of(itemTwoDTO));

        when(mealPlanShoppingCategoryRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlanId))
                .thenReturn(List.of(categoryOne, categoryTwo));
        when(mealPlanShoppingItemRepository.findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(categoryOneId))
                .thenReturn(List.of(itemOneEntity));
        when(mealPlanShoppingItemRepository.findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(categoryTwoId))
                .thenReturn(List.of(itemTwoEntity));
        when(mealPlanMapper.toShoppingItemDTOs(List.of(itemOneEntity))).thenReturn(List.of(itemOneDTO));
        when(mealPlanMapper.toShoppingItemDTOs(List.of(itemTwoEntity))).thenReturn(List.of(itemTwoDTO));
        when(mealPlanMapper.toShoppingCategoryDTO(categoryOne, List.of(itemOneDTO))).thenReturn(categoryOneDTO);
        when(mealPlanMapper.toShoppingCategoryDTO(categoryTwo, List.of(itemTwoDTO))).thenReturn(categoryTwoDTO);

        final List<MealPlanShoppingCategoryDTO> result = mealPlanShoppingListAssembler.assemble(mealPlanId);

        assertEquals(List.of(categoryOneDTO, categoryTwoDTO), result);
    }

    @Test
    @DisplayName("assemble returns an empty list when the meal plan has no shopping categories")
    void assemble_NoCategories_ReturnsEmptyList() {
        final Long mealPlanId = 1L;
        when(mealPlanShoppingCategoryRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlanId)).thenReturn(List.of());

        final List<MealPlanShoppingCategoryDTO> result = mealPlanShoppingListAssembler.assemble(mealPlanId);

        assertEquals(List.of(), result);
    }
}
