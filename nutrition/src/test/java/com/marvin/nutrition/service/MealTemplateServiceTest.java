package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealEntryRequest;
import com.marvin.nutrition.dto.CreateMealTemplateRequest;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.MealTemplateDTO;
import com.marvin.nutrition.dto.MealTemplateItemDTO;
import com.marvin.nutrition.dto.MealTemplateItemRequest;
import com.marvin.nutrition.dto.UpdateMealTemplateRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealTemplateEntity;
import com.marvin.nutrition.entity.MealTemplateItemEntity;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.mapper.MealTemplateMapper;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealTemplateItemRepository;
import com.marvin.nutrition.repository.MealTemplateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import reactor.test.StepVerifier;

/** Unit tests for {@link MealTemplateService} covering CRUD and macro computation. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealTemplateService Tests")
class MealTemplateServiceTest {

    @Mock
    private MealTemplateRepository mealTemplateRepository;

    @Mock
    private MealTemplateItemRepository mealTemplateItemRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private MealTemplateMapper mealTemplateMapper;

    @Mock
    private MealTemplateWriteService mealTemplateWriteService;

    @Mock
    private MealEntryWriteService mealEntryWriteService;

    @InjectMocks
    private MealTemplateService mealTemplateService;

    private UUID templateId;
    private UUID itemId;
    private UUID foodId;
    private FoodEntity foodEntity;
    private MealTemplateEntity templateEntity;
    private MealTemplateItemEntity itemEntity;
    private MealTemplateItemDTO itemDTO;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        itemId = UUID.randomUUID();
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

        itemEntity = new MealTemplateItemEntity();
        itemEntity.setId(itemId);
        itemEntity.setMealTemplateId(templateId);
        itemEntity.setFoodId(foodId);
        itemEntity.setQuantityG(new BigDecimal("50"));

        // kcalPer100=370, qty=50 -> 185.00; protein 6.50; carbs 30.00; fat 3.50
        itemDTO = new MealTemplateItemDTO(
                itemId, foodId, "Oatmeal", new BigDecimal("50"),
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50")
        );
    }

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAll returns templates ordered by name with live-computed item macros")
    void findAll_ReturnsTemplatesWithMacros() {
        when(mealTemplateRepository.findAllByOrderByNameAsc()).thenReturn(List.of(templateEntity));
        when(mealTemplateItemRepository.findByMealTemplateId(templateId)).thenReturn(List.of(itemEntity));
        when(foodRepository.findAllById(List.of(foodId))).thenReturn(List.of(foodEntity));
        when(mealTemplateMapper.toItemDTO(
                itemEntity, "Oatmeal",
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50")
        )).thenReturn(itemDTO);

        StepVerifier.create(mealTemplateService.findAll())
                .assertNext(list -> {
                    assertEquals(1, list.size());
                    final MealTemplateDTO dto = list.get(0);
                    assertEquals(templateId, dto.id());
                    assertEquals("Breakfast Bowl", dto.name());
                    assertEquals(1, dto.items().size());
                    assertEquals(itemDTO, dto.items().get(0));
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // findById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findById returns template with live-computed item macros")
    void findById_Found_ReturnsTemplateWithMacros() {
        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));
        when(mealTemplateItemRepository.findByMealTemplateId(templateId)).thenReturn(List.of(itemEntity));
        when(foodRepository.findAllById(List.of(foodId))).thenReturn(List.of(foodEntity));
        when(mealTemplateMapper.toItemDTO(
                itemEntity, "Oatmeal",
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50")
        )).thenReturn(itemDTO);

        StepVerifier.create(mealTemplateService.findById(templateId))
                .assertNext(dto -> {
                    assertEquals(templateId, dto.id());
                    assertEquals("Breakfast Bowl", dto.name());
                    assertEquals(List.of(itemDTO), dto.items());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findById emits NoSuchElementException when template not found")
    void findById_NotFound_EmitsNoSuchElementException() {
        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        StepVerifier.create(mealTemplateService.findById(templateId))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    // -----------------------------------------------------------------------
    // create
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("create delegates to write service and assembles response DTO")
    void create_DelegatesAndAssemblesDTO() {
        final CreateMealTemplateRequest req = new CreateMealTemplateRequest(
                "Breakfast Bowl", List.of(new MealTemplateItemRequest(foodId, new BigDecimal("50")))
        );
        final MealTemplateWriteService.MealTemplateWithItems writeResult =
                new MealTemplateWriteService.MealTemplateWithItems(templateEntity, List.of(itemEntity));

        when(mealTemplateWriteService.create(req)).thenReturn(writeResult);
        when(foodRepository.findAllById(List.of(foodId))).thenReturn(List.of(foodEntity));
        when(mealTemplateMapper.toItemDTO(
                itemEntity, "Oatmeal",
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50")
        )).thenReturn(itemDTO);

        StepVerifier.create(mealTemplateService.create(req))
                .assertNext(dto -> {
                    assertEquals(templateId, dto.id());
                    assertEquals("Breakfast Bowl", dto.name());
                    assertEquals(List.of(itemDTO), dto.items());
                })
                .verifyComplete();

        verify(mealTemplateWriteService).create(req);
    }

    // -----------------------------------------------------------------------
    // update
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("update delegates to write service and assembles response DTO")
    void update_DelegatesAndAssemblesDTO() {
        final UpdateMealTemplateRequest req = new UpdateMealTemplateRequest(
                "Renamed Bowl", List.of(new MealTemplateItemRequest(foodId, new BigDecimal("50")))
        );
        final MealTemplateEntity renamed = new MealTemplateEntity();
        renamed.setId(templateId);
        renamed.setName("Renamed Bowl");

        final MealTemplateItemDTO renamedItemDTO = new MealTemplateItemDTO(
                itemId, foodId, "Oatmeal", new BigDecimal("50"),
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50")
        );
        final MealTemplateWriteService.MealTemplateWithItems writeResult =
                new MealTemplateWriteService.MealTemplateWithItems(renamed, List.of(itemEntity));

        when(mealTemplateWriteService.update(templateId, req)).thenReturn(writeResult);
        when(foodRepository.findAllById(List.of(foodId))).thenReturn(List.of(foodEntity));
        when(mealTemplateMapper.toItemDTO(
                itemEntity, "Oatmeal",
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50")
        )).thenReturn(renamedItemDTO);

        StepVerifier.create(mealTemplateService.update(templateId, req))
                .assertNext(dto -> {
                    assertEquals(templateId, dto.id());
                    assertEquals("Renamed Bowl", dto.name());
                    assertEquals(List.of(renamedItemDTO), dto.items());
                })
                .verifyComplete();

        verify(mealTemplateWriteService).update(templateId, req);
    }

    @Test
    @DisplayName("update emits NoSuchElementException when write service throws")
    void update_NotFound_EmitsNoSuchElementException() {
        final UpdateMealTemplateRequest req = new UpdateMealTemplateRequest("Renamed Bowl", List.of());

        when(mealTemplateWriteService.update(templateId, req))
                .thenThrow(new NoSuchElementException("Meal template not found: " + templateId));

        StepVerifier.create(mealTemplateService.update(templateId, req))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("delete delegates to write service and completes")
    void delete_DelegatesToWriteService() {
        StepVerifier.create(mealTemplateService.delete(templateId))
                .verifyComplete();

        verify(mealTemplateWriteService).delete(templateId);
    }

    @Test
    @DisplayName("delete emits NoSuchElementException when write service throws")
    void delete_NotFound_EmitsNoSuchElementException() {
        doThrow(new NoSuchElementException("Meal template not found: " + templateId))
                .when(mealTemplateWriteService).delete(templateId);

        StepVerifier.create(mealTemplateService.delete(templateId))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("findAll returns empty list when no templates exist")
    void findAll_NoTemplates_ReturnsEmptyList() {
        when(mealTemplateRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(foodRepository.findAllById(List.of())).thenReturn(List.of());

        StepVerifier.create(mealTemplateService.findAll())
                .assertNext(list -> assertEquals(0, list.size()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // logToDay
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("logToDay maps template items to create requests and delegates to MealEntryWriteService")
    void logToDay_TemplateWithItems_DelegatesToMealEntryWriteService() {
        final LocalDate date = LocalDate.of(2026, 6, 7);
        final CreateMealEntryRequest expectedRequest = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, itemEntity.getQuantityG(), null, null, null, null, null
        );
        final MealEntryDTO createdEntry = new MealEntryDTO(
                UUID.randomUUID(), date, MealType.LUNCH, foodId, null, itemEntity.getQuantityG(),
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50"), "Oatmeal"
        );

        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));
        when(mealTemplateItemRepository.findByMealTemplateId(templateId)).thenReturn(List.of(itemEntity));
        when(mealEntryWriteService.createEntries(date, List.of(expectedRequest)))
                .thenReturn(List.of(createdEntry));

        StepVerifier.create(mealTemplateService.logToDay(date, templateId, MealType.LUNCH))
                .assertNext(created -> assertEquals(List.of(createdEntry), created))
                .verifyComplete();

        verify(mealEntryWriteService).createEntries(date, List.of(expectedRequest));
    }

    @Test
    @DisplayName("logToDay returns an empty list without calling MealEntryWriteService when template has no items")
    void logToDay_TemplateWithoutItems_ReturnsEmptyList() {
        final LocalDate date = LocalDate.of(2026, 6, 7);

        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));
        when(mealTemplateItemRepository.findByMealTemplateId(templateId)).thenReturn(List.of());

        StepVerifier.create(mealTemplateService.logToDay(date, templateId, MealType.LUNCH))
                .assertNext(created -> assertEquals(List.of(), created))
                .verifyComplete();

        verify(mealEntryWriteService, never()).createEntries(any(), any());
    }

    @Test
    @DisplayName("logToDay emits NoSuchElementException when template not found")
    void logToDay_TemplateNotFound_EmitsNoSuchElementException() {
        final LocalDate date = LocalDate.of(2026, 6, 7);

        when(mealTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        StepVerifier.create(mealTemplateService.logToDay(date, templateId, MealType.LUNCH))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}
