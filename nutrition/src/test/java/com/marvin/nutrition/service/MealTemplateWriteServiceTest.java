package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealTemplateRequest;
import com.marvin.nutrition.dto.MealTemplateItemRequest;
import com.marvin.nutrition.dto.UpdateMealTemplateRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealTemplateEntity;
import com.marvin.nutrition.entity.MealTemplateItemEntity;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealTemplateItemRepository;
import com.marvin.nutrition.repository.MealTemplateRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link MealTemplateWriteService} covering the transactional write operations. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealTemplateWriteService Tests")
class MealTemplateWriteServiceTest {

    @Mock
    private MealTemplateRepository mealTemplateRepository;

    @Mock
    private MealTemplateItemRepository mealTemplateItemRepository;

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private MealTemplateWriteService mealTemplateWriteService;

    private UUID templateId;
    private UUID foodId;
    private FoodEntity foodEntity;
    private MealTemplateEntity templateEntity;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        foodId = UUID.randomUUID();

        foodEntity = new FoodEntity();
        foodEntity.setId(foodId);
        foodEntity.setName("Oatmeal");
        foodEntity.setKcalPer100(new BigDecimal("370.00"));
        foodEntity.setProteinPer100(new BigDecimal("13.00"));
        foodEntity.setCarbsPer100(new BigDecimal("60.00"));
        foodEntity.setFatPer100(new BigDecimal("7.00"));

        templateEntity = new MealTemplateEntity();
        templateEntity.setId(templateId);
        templateEntity.setName("Breakfast Bowl");
    }

    // -----------------------------------------------------------------------
    // create
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("create saves template and its items when all foodIds exist")
    void create_AllFoodsExist_SavesTemplateAndItems() {
        final CreateMealTemplateRequest req = new CreateMealTemplateRequest(
                "Breakfast Bowl", List.of(new MealTemplateItemRequest(foodId, new BigDecimal("50")))
        );

        when(foodRepository.findAllById(List.of(foodId))).thenReturn(List.of(foodEntity));
        when(mealTemplateRepository.save(any(MealTemplateEntity.class))).thenReturn(templateEntity);
        when(mealTemplateItemRepository.save(any(MealTemplateItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MealTemplateWriteService.MealTemplateWithItems result = mealTemplateWriteService.create(req);

        assertEquals(templateEntity, result.template());
        assertEquals(1, result.items().size());
        verify(mealTemplateRepository).save(argThat(e -> "Breakfast Bowl".equals(e.getName())));
        verify(mealTemplateItemRepository).save(argThat(item ->
                templateId.equals(item.getMealTemplateId())
                        && foodId.equals(item.getFoodId())
                        && new BigDecimal("50").compareTo(item.getQuantityG()) == 0
        ));
    }

    @Test
    @DisplayName("create allows an empty items list")
    void create_EmptyItems_SavesTemplateWithNoItems() {
        final CreateMealTemplateRequest req = new CreateMealTemplateRequest("Empty Template", List.of());

        when(foodRepository.findAllById(List.of())).thenReturn(List.of());
        when(mealTemplateRepository.save(any(MealTemplateEntity.class))).thenReturn(templateEntity);

        final MealTemplateWriteService.MealTemplateWithItems result = mealTemplateWriteService.create(req);

        assertEquals(templateEntity, result.template());
        assertEquals(0, result.items().size());
        verify(mealTemplateItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("create throws NoSuchElementException when a referenced food does not exist")
    void create_UnknownFoodId_ThrowsNoSuchElementException() {
        final UUID unknownFoodId = UUID.randomUUID();
        final CreateMealTemplateRequest req = new CreateMealTemplateRequest(
                "Breakfast Bowl", List.of(new MealTemplateItemRequest(unknownFoodId, new BigDecimal("50")))
        );

        when(foodRepository.findAllById(List.of(unknownFoodId))).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> mealTemplateWriteService.create(req));

        verify(mealTemplateRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // update
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("update renames template, replaces items, and validates foodIds")
    void update_Found_RenamesAndReplacesItems() {
        final UpdateMealTemplateRequest req = new UpdateMealTemplateRequest(
                "Renamed Bowl", List.of(new MealTemplateItemRequest(foodId, new BigDecimal("80")))
        );

        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));
        when(foodRepository.findAllById(List.of(foodId))).thenReturn(List.of(foodEntity));
        when(mealTemplateRepository.save(any(MealTemplateEntity.class))).thenReturn(templateEntity);
        when(mealTemplateItemRepository.save(any(MealTemplateItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MealTemplateWriteService.MealTemplateWithItems result = mealTemplateWriteService.update(templateId, req);

        assertEquals("Renamed Bowl", result.template().getName());
        assertEquals(1, result.items().size());
        verify(mealTemplateItemRepository).deleteByMealTemplateId(templateId);
        verify(mealTemplateItemRepository).save(argThat(item ->
                templateId.equals(item.getMealTemplateId())
                        && foodId.equals(item.getFoodId())
                        && new BigDecimal("80").compareTo(item.getQuantityG()) == 0
        ));
    }

    @Test
    @DisplayName("update throws NoSuchElementException when template not found")
    void update_NotFound_ThrowsNoSuchElementException() {
        final UpdateMealTemplateRequest req = new UpdateMealTemplateRequest("Renamed Bowl", List.of());

        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> mealTemplateWriteService.update(templateId, req));

        verify(mealTemplateItemRepository, never()).deleteByMealTemplateId(any());
        verify(mealTemplateRepository, never()).save(any());
    }

    @Test
    @DisplayName("update throws NoSuchElementException when a referenced food does not exist")
    void update_UnknownFoodId_ThrowsNoSuchElementException() {
        final UUID unknownFoodId = UUID.randomUUID();
        final UpdateMealTemplateRequest req = new UpdateMealTemplateRequest(
                "Renamed Bowl", List.of(new MealTemplateItemRequest(unknownFoodId, new BigDecimal("50")))
        );

        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));
        when(foodRepository.findAllById(List.of(unknownFoodId))).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> mealTemplateWriteService.update(templateId, req));

        verify(mealTemplateItemRepository, never()).deleteByMealTemplateId(any());
        verify(mealTemplateRepository, never()).save(any());
    }

    @Test
    @DisplayName("update with empty items list removes all items")
    void update_EmptyItems_RemovesAllItems() {
        final UpdateMealTemplateRequest req = new UpdateMealTemplateRequest("Renamed Bowl", List.of());

        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));
        when(foodRepository.findAllById(List.of())).thenReturn(List.of());
        when(mealTemplateRepository.save(any(MealTemplateEntity.class))).thenReturn(templateEntity);

        final MealTemplateWriteService.MealTemplateWithItems result = mealTemplateWriteService.update(templateId, req);

        assertEquals(0, result.items().size());
        verify(mealTemplateItemRepository).deleteByMealTemplateId(templateId);
        verify(mealTemplateItemRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("delete throws NoSuchElementException when template not found")
    void delete_NotFound_ThrowsNoSuchElementException() {
        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> mealTemplateWriteService.delete(templateId));

        verify(mealTemplateRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete removes template when found")
    void delete_Found_RemovesTemplate() {
        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));

        mealTemplateWriteService.delete(templateId);

        verify(mealTemplateRepository, times(1)).delete(templateEntity);
    }
}
