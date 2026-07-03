package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingCategoryRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingItemRequest;
import com.marvin.nutrition.entity.MealPlanShoppingCategoryEntity;
import com.marvin.nutrition.entity.MealPlanShoppingItemEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanShoppingCategoryRepository;
import com.marvin.nutrition.repository.MealPlanShoppingItemRepository;
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

/** Unit tests for {@link MealPlanShoppingListWriteService} covering shopping category and item content updates. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanShoppingListWriteService Tests")
class MealPlanShoppingListWriteServiceTest {

    @Mock
    private MealPlanShoppingCategoryRepository mealPlanShoppingCategoryRepository;

    @Mock
    private MealPlanShoppingItemRepository mealPlanShoppingItemRepository;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @InjectMocks
    private MealPlanShoppingListWriteService mealPlanShoppingListWriteService;

    // -----------------------------------------------------------------------
    // updateShoppingCategory
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateShoppingCategory applies non-null fields, saves and returns the category with its items")
    void updateShoppingCategory_AppliesNonNullFields_ReturnsUpdatedCategoryWithItems() {
        final UUID categoryId = UUID.randomUUID();
        final MealPlanShoppingCategoryEntity category = new MealPlanShoppingCategoryEntity();
        category.setId(categoryId);
        category.setTitle("old title");

        final MealPlanShoppingItemEntity itemEntity = new MealPlanShoppingItemEntity();
        itemEntity.setId(UUID.randomUUID());

        final MealPlanShoppingItemDTO itemDTO =
                new MealPlanShoppingItemDTO(itemEntity.getId(), "Hähnchenbrustfilet", null, null, null, "1.200 g");
        final MealPlanShoppingCategoryDTO categoryDTO =
                new MealPlanShoppingCategoryDTO(categoryId, "new title", List.of(itemDTO));

        final UpdateMealPlanShoppingCategoryRequest req = new UpdateMealPlanShoppingCategoryRequest("new title");

        when(mealPlanShoppingCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(mealPlanShoppingCategoryRepository.save(category)).thenReturn(category);
        when(mealPlanShoppingItemRepository.findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(categoryId))
                .thenReturn(List.of(itemEntity));
        when(mealPlanMapper.toShoppingItemDTOs(List.of(itemEntity))).thenReturn(List.of(itemDTO));
        when(mealPlanMapper.toShoppingCategoryDTO(category, List.of(itemDTO))).thenReturn(categoryDTO);

        final MealPlanShoppingCategoryDTO result =
                mealPlanShoppingListWriteService.updateShoppingCategory(categoryId, req);

        assertEquals("new title", category.getTitle());
        assertEquals(categoryDTO, result);
    }

    @Test
    @DisplayName("updateShoppingCategory throws NoSuchElementException when the category does not exist")
    void updateShoppingCategory_NotFound_ThrowsNoSuchElementException() {
        final UUID categoryId = UUID.randomUUID();
        when(mealPlanShoppingCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        final UpdateMealPlanShoppingCategoryRequest req = new UpdateMealPlanShoppingCategoryRequest("title");

        assertThrows(NoSuchElementException.class,
                () -> mealPlanShoppingListWriteService.updateShoppingCategory(categoryId, req));
    }

    // -----------------------------------------------------------------------
    // updateShoppingItem
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateShoppingItem applies non-null fields, saves and returns the updated item")
    void updateShoppingItem_AppliesNonNullFields_ReturnsUpdatedItem() {
        final UUID itemId = UUID.randomUUID();
        final MealPlanShoppingItemEntity item = new MealPlanShoppingItemEntity();
        item.setId(itemId);
        item.setName("old name");
        item.setQty("500 g");

        final MealPlanShoppingItemDTO itemDTO = new MealPlanShoppingItemDTO(itemId, "new name", null, null, null, "500 g");

        final UpdateMealPlanShoppingItemRequest req = new UpdateMealPlanShoppingItemRequest("new name", null, null, null, null);

        when(mealPlanShoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(mealPlanShoppingItemRepository.save(item)).thenReturn(item);
        when(mealPlanMapper.toShoppingItemDTO(item)).thenReturn(itemDTO);

        final MealPlanShoppingItemDTO result = mealPlanShoppingListWriteService.updateShoppingItem(itemId, req);

        assertEquals("new name", item.getName());
        assertEquals("500 g", item.getQty());
        assertEquals(itemDTO, result);
    }

    @Test
    @DisplayName("updateShoppingItem throws NoSuchElementException when the item does not exist")
    void updateShoppingItem_NotFound_ThrowsNoSuchElementException() {
        final UUID itemId = UUID.randomUUID();
        when(mealPlanShoppingItemRepository.findById(itemId)).thenReturn(Optional.empty());

        final UpdateMealPlanShoppingItemRequest req = new UpdateMealPlanShoppingItemRequest("name", null, null, null, null);

        assertThrows(NoSuchElementException.class,
                () -> mealPlanShoppingListWriteService.updateShoppingItem(itemId, req));
    }
}
