package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealPlanChangelogEntryRequest;
import com.marvin.nutrition.dto.MealPlanChangelogEntryDTO;
import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.dto.MealPlanFooterDTO;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.MealPlanShoppingListDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.MealPlanStatDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingCategoryRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingItemRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
import com.marvin.nutrition.dto.UpdateMealPlanStatRequest;
import com.marvin.nutrition.service.MealPlanService;
import com.marvin.nutrition.service.MealPlanWriteService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link MealPlanController} covering the meal-plan read endpoint and its content-write endpoints. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanController Tests")
class MealPlanControllerTest {

    @Mock
    private MealPlanService mealPlanService;

    @Mock
    private MealPlanWriteService mealPlanWriteService;

    @InjectMocks
    private MealPlanController mealPlanController;

    private MealPlanDTO mealPlanDTO;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        final MealPlanShoppingListDTO shoppingList = new MealPlanShoppingListDTO(
                "4 · Einkaufsliste für Lidl (1 Woche)", "note", List.of(), null
        );
        final MealPlanFooterDTO footer = new MealPlanFooterDTO("note", List.of());

        mealPlanDTO = new MealPlanDTO(
                "Version 2", "Ernährungsplan & Einkaufsliste", "description",
                List.of(), List.of(), List.of(), shoppingList, footer
        );
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/meal-plan
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getMealPlan returns 200 with the meal plan document")
    void getMealPlan_Returns200WithMealPlan() {
        when(mealPlanService.getMealPlan()).thenReturn(Mono.just(mealPlanDTO));

        final Mono<MealPlanDTO> result = mealPlanController.getMealPlan();

        StepVerifier.create(result)
                .assertNext(dto -> assertEquals("Ernährungsplan & Einkaufsliste", dto.title()))
                .verifyComplete();

        verify(mealPlanService).getMealPlan();
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-plan
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateMealPlan returns 200 with the updated header")
    void updateMealPlan_Returns200WithUpdatedHeader() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("new eyebrow", null, null, null, null, null, null);
        final MealPlanHeaderDTO headerDTO = new MealPlanHeaderDTO(
                "new eyebrow", "title", "description", "shopping title", "shopping note", null, "footer");
        when(mealPlanWriteService.updateMealPlan(req)).thenReturn(headerDTO);

        final Mono<ResponseEntity<MealPlanHeaderDTO>> result = mealPlanController.updateMealPlan(req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(headerDTO, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateMealPlan returns 404 when the meal plan is missing")
    void updateMealPlan_NotFound_Returns404() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("eyebrow", null, null, null, null, null, null);
        when(mealPlanWriteService.updateMealPlan(req)).thenThrow(new NoSuchElementException("Meal plan not found"));

        final Mono<ResponseEntity<MealPlanHeaderDTO>> result = mealPlanController.updateMealPlan(req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-plan/sections/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateSection returns 200 with the updated section")
    void updateSection_Returns200WithUpdatedSection() {
        final UUID sectionId = UUID.randomUUID();
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("new title", null, null);
        final MealPlanSectionDTO sectionDTO = new MealPlanSectionDTO(sectionId, "new title", "note", List.of(), null, null);
        when(mealPlanWriteService.updateSection(eq(sectionId), any(UpdateMealPlanSectionRequest.class)))
                .thenReturn(sectionDTO);

        final Mono<ResponseEntity<MealPlanSectionDTO>> result = mealPlanController.updateSection(sectionId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(sectionDTO, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateSection returns 404 when the section does not exist")
    void updateSection_NotFound_Returns404() {
        final UUID sectionId = UUID.randomUUID();
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("title", null, null);
        when(mealPlanWriteService.updateSection(eq(sectionId), any(UpdateMealPlanSectionRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanSectionDTO>> result = mealPlanController.updateSection(sectionId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-plan/rows/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateRow returns 200 with the updated row")
    void updateRow_Returns200WithUpdatedRow() {
        final UUID rowId = UUID.randomUUID();
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest("meal", null, null, null, null);
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(rowId, "meal", "details", "qty", "kcal", "protein");
        when(mealPlanWriteService.updateRow(eq(rowId), any(UpdateMealPlanRowRequest.class))).thenReturn(rowDTO);

        final Mono<ResponseEntity<MealPlanRowDTO>> result = mealPlanController.updateRow(rowId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(rowDTO, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateRow returns 404 when the row does not exist")
    void updateRow_NotFound_Returns404() {
        final UUID rowId = UUID.randomUUID();
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest("meal", null, null, null, null);
        when(mealPlanWriteService.updateRow(eq(rowId), any(UpdateMealPlanRowRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanRowDTO>> result = mealPlanController.updateRow(rowId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-plan/stats/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateStat returns 200 with the updated stat")
    void updateStat_Returns200WithUpdatedStat() {
        final UUID statId = UUID.randomUUID();
        final UpdateMealPlanStatRequest req = new UpdateMealPlanStatRequest("label", null);
        final MealPlanStatDTO statDTO = new MealPlanStatDTO(statId, "label", "value");
        when(mealPlanWriteService.updateStat(eq(statId), any(UpdateMealPlanStatRequest.class))).thenReturn(statDTO);

        final Mono<ResponseEntity<MealPlanStatDTO>> result = mealPlanController.updateStat(statId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(statDTO, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateStat returns 404 when the stat does not exist")
    void updateStat_NotFound_Returns404() {
        final UUID statId = UUID.randomUUID();
        final UpdateMealPlanStatRequest req = new UpdateMealPlanStatRequest("label", null);
        when(mealPlanWriteService.updateStat(eq(statId), any(UpdateMealPlanStatRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanStatDTO>> result = mealPlanController.updateStat(statId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-plan/shopping-categories/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateShoppingCategory returns 200 with the updated category")
    void updateShoppingCategory_Returns200WithUpdatedCategory() {
        final UUID categoryId = UUID.randomUUID();
        final UpdateMealPlanShoppingCategoryRequest req = new UpdateMealPlanShoppingCategoryRequest("title");
        final MealPlanShoppingCategoryDTO categoryDTO = new MealPlanShoppingCategoryDTO(categoryId, "title", List.of());
        when(mealPlanWriteService.updateShoppingCategory(eq(categoryId), any(UpdateMealPlanShoppingCategoryRequest.class)))
                .thenReturn(categoryDTO);

        final Mono<ResponseEntity<MealPlanShoppingCategoryDTO>> result =
                mealPlanController.updateShoppingCategory(categoryId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(categoryDTO, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateShoppingCategory returns 404 when the category does not exist")
    void updateShoppingCategory_NotFound_Returns404() {
        final UUID categoryId = UUID.randomUUID();
        final UpdateMealPlanShoppingCategoryRequest req = new UpdateMealPlanShoppingCategoryRequest("title");
        when(mealPlanWriteService.updateShoppingCategory(eq(categoryId), any(UpdateMealPlanShoppingCategoryRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanShoppingCategoryDTO>> result =
                mealPlanController.updateShoppingCategory(categoryId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-plan/shopping-items/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateShoppingItem returns 200 with the updated item")
    void updateShoppingItem_Returns200WithUpdatedItem() {
        final UUID itemId = UUID.randomUUID();
        final UpdateMealPlanShoppingItemRequest req = new UpdateMealPlanShoppingItemRequest("name", null, null, null, null);
        final MealPlanShoppingItemDTO itemDTO = new MealPlanShoppingItemDTO(itemId, "name", null, null, null, "qty");
        when(mealPlanWriteService.updateShoppingItem(eq(itemId), any(UpdateMealPlanShoppingItemRequest.class)))
                .thenReturn(itemDTO);

        final Mono<ResponseEntity<MealPlanShoppingItemDTO>> result = mealPlanController.updateShoppingItem(itemId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(itemDTO, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateShoppingItem returns 404 when the item does not exist")
    void updateShoppingItem_NotFound_Returns404() {
        final UUID itemId = UUID.randomUUID();
        final UpdateMealPlanShoppingItemRequest req = new UpdateMealPlanShoppingItemRequest("name", null, null, null, null);
        when(mealPlanWriteService.updateShoppingItem(eq(itemId), any(UpdateMealPlanShoppingItemRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanShoppingItemDTO>> result = mealPlanController.updateShoppingItem(itemId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-plan/sources/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateSource returns 200 with the updated source")
    void updateSource_Returns200WithUpdatedSource() {
        final UUID sourceId = UUID.randomUUID();
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("label", null);
        final MealPlanSourceDTO sourceDTO = new MealPlanSourceDTO(sourceId, "label", "url");
        when(mealPlanWriteService.updateSource(eq(sourceId), any(UpdateMealPlanSourceRequest.class)))
                .thenReturn(sourceDTO);

        final Mono<ResponseEntity<MealPlanSourceDTO>> result = mealPlanController.updateSource(sourceId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(sourceDTO, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateSource returns 404 when the source does not exist")
    void updateSource_NotFound_Returns404() {
        final UUID sourceId = UUID.randomUUID();
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("label", null);
        when(mealPlanWriteService.updateSource(eq(sourceId), any(UpdateMealPlanSourceRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanSourceDTO>> result = mealPlanController.updateSource(sourceId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // POST /nutrition/meal-plan/changelog
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addChangelogEntry returns 201 Created with Location header pointing to new entry")
    void addChangelogEntry_Returns201WithLocation() {
        final UUID entryId = UUID.randomUUID();
        final CreateMealPlanChangelogEntryRequest req =
                new CreateMealPlanChangelogEntryRequest("Whey", "80 g/Tag", "-> 40 g/Tag", 0);
        final MealPlanChangelogEntryDTO entryDTO = new MealPlanChangelogEntryDTO(entryId, "Whey", "80 g/Tag", "-> 40 g/Tag");
        when(mealPlanWriteService.addChangelogEntry(any(CreateMealPlanChangelogEntryRequest.class)))
                .thenReturn(entryDTO);

        final Mono<ResponseEntity<MealPlanChangelogEntryDTO>> result = mealPlanController.addChangelogEntry(req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(entryDTO, response.getBody());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(response.getHeaders().getLocation().toString().contains(entryId.toString()));
                })
                .verifyComplete();

        verify(mealPlanWriteService).addChangelogEntry(any(CreateMealPlanChangelogEntryRequest.class));
    }
}
