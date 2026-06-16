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
import com.marvin.nutrition.dto.SaveEstimateAsTemplateRequest;
import com.marvin.nutrition.dto.UpdateMealTemplateRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.FoodSource;
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
    // createFromEstimate
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createFromEstimate saves food, template and item with correct field values")
    void createFromEstimate_SavesFoodTemplateAndItem() {
        final SaveEstimateAsTemplateRequest req = new SaveEstimateAsTemplateRequest(
                "Canteen Lunch",
                new BigDecimal("650"),
                new BigDecimal("35"),
                new BigDecimal("70"),
                new BigDecimal("20")
        );
        final UUID savedFoodId = UUID.randomUUID();
        final FoodEntity savedFood = new FoodEntity();
        savedFood.setId(savedFoodId);

        when(foodRepository.save(any(FoodEntity.class))).thenReturn(savedFood);
        when(mealTemplateRepository.save(any(MealTemplateEntity.class))).thenReturn(templateEntity);
        when(mealTemplateItemRepository.save(any(MealTemplateItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final MealTemplateWriteService.MealTemplateWithItems result = mealTemplateWriteService.createFromEstimate(req);

        assertEquals(templateEntity, result.template());
        assertEquals(1, result.items().size());

        verify(foodRepository).save(argThat(food ->
                "Canteen Lunch".equals(food.getName())
                        && food.getBrand() == null
                        && FoodSource.ESTIMATE.equals(food.getSource())
                        && new BigDecimal("650").compareTo(food.getKcalPer100()) == 0
                        && new BigDecimal("35").compareTo(food.getProteinPer100()) == 0
                        && new BigDecimal("70").compareTo(food.getCarbsPer100()) == 0
                        && new BigDecimal("20").compareTo(food.getFatPer100()) == 0
                        && BigDecimal.valueOf(100).compareTo(food.getDefaultServingG()) == 0
        ));
        verify(mealTemplateRepository).save(argThat(t -> "Canteen Lunch".equals(t.getName())));
        verify(mealTemplateItemRepository).save(argThat(item ->
                templateId.equals(item.getMealTemplateId())
                        && savedFoodId.equals(item.getFoodId())
                        && BigDecimal.valueOf(100).compareTo(item.getQuantityG()) == 0
        ));
    }

    @Test
    @DisplayName("createFromEstimate does not save template or item when food save fails")
    void createFromEstimate_FoodSaveFails_DoesNotSaveTemplateOrItem() {
        final SaveEstimateAsTemplateRequest req = new SaveEstimateAsTemplateRequest(
                "Canteen Lunch",
                new BigDecimal("650"),
                new BigDecimal("35"),
                new BigDecimal("70"),
                new BigDecimal("20")
        );

        when(foodRepository.save(any(FoodEntity.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> mealTemplateWriteService.createFromEstimate(req));

        verify(mealTemplateRepository, never()).save(any());
        verify(mealTemplateItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("createFromEstimate does not save item when template save fails")
    void createFromEstimate_TemplateSaveFails_DoesNotSaveItem() {
        final SaveEstimateAsTemplateRequest req = new SaveEstimateAsTemplateRequest(
                "Canteen Lunch",
                new BigDecimal("650"),
                new BigDecimal("35"),
                new BigDecimal("70"),
                new BigDecimal("20")
        );
        final FoodEntity savedFood = new FoodEntity();
        savedFood.setId(UUID.randomUUID());

        when(foodRepository.save(any(FoodEntity.class))).thenReturn(savedFood);
        when(mealTemplateRepository.save(any(MealTemplateEntity.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> mealTemplateWriteService.createFromEstimate(req));

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
