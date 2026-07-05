package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealPlanRowRequest;
import com.marvin.nutrition.dto.CreateMealPlanRowsRequest;
import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.dto.MealPlanFooterDTO;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.service.MealPlanService;
import com.marvin.nutrition.service.MealPlanWriteService;
import java.math.BigDecimal;
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
        final MealPlanFooterDTO footer = new MealPlanFooterDTO("note", List.of());

        mealPlanDTO = new MealPlanDTO(
                "Version 4", "Ernährungsplan & Einkaufsliste", "description", List.of(), footer
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
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("new eyebrow", null, null, null);
        final MealPlanHeaderDTO headerDTO = new MealPlanHeaderDTO("new eyebrow", "title", "description", "footer");
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
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("eyebrow", null, null, null);
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
        final MealPlanSectionDTO sectionDTO = new MealPlanSectionDTO(sectionId, "new title", "note", List.of(), null);
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
    // POST /nutrition/meal-plan/sections/{sectionId}/rows
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addRow returns 201 Created with Location header pointing to the new row")
    void addRow_Returns201WithLocation() {
        final UUID sectionId = UUID.randomUUID();
        final UUID rowId = UUID.randomUUID();
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(
                rowId, MealType.BREAKFAST, req.foodId(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));
        when(mealPlanWriteService.addRow(eq(sectionId), any(CreateMealPlanRowRequest.class))).thenReturn(rowDTO);

        final Mono<ResponseEntity<MealPlanRowDTO>> result = mealPlanController.addRow(sectionId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertEquals(rowDTO, response.getBody());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(response.getHeaders().getLocation().toString().contains(rowId.toString()));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("addRow returns 404 when the section or referenced food does not exist")
    void addRow_NotFound_Returns404() {
        final UUID sectionId = UUID.randomUUID();
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));
        when(mealPlanWriteService.addRow(eq(sectionId), any(CreateMealPlanRowRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanRowDTO>> result = mealPlanController.addRow(sectionId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // POST /nutrition/meal-plan/sections/{sectionId}/rows/batch
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addRows returns 201 Created with a JSON array body and no Location header")
    void addRows_Returns201WithArrayBody() {
        final UUID sectionId = UUID.randomUUID();
        final CreateMealPlanRowRequest rowReq =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));
        final CreateMealPlanRowsRequest req = new CreateMealPlanRowsRequest(List.of(rowReq));
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(
                UUID.randomUUID(), MealType.BREAKFAST, rowReq.foodId(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));
        when(mealPlanWriteService.addRows(eq(sectionId), any())).thenReturn(List.of(rowDTO));

        final Mono<ResponseEntity<List<MealPlanRowDTO>>> result = mealPlanController.addRows(sectionId, req);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertEquals(List.of(rowDTO), response.getBody());
                    assertNull(response.getHeaders().getLocation());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("addRows returns 404 when the section or any referenced food does not exist")
    void addRows_NotFound_Returns404() {
        final UUID sectionId = UUID.randomUUID();
        final CreateMealPlanRowRequest rowReq =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));
        final CreateMealPlanRowsRequest req = new CreateMealPlanRowsRequest(List.of(rowReq));
        when(mealPlanWriteService.addRows(eq(sectionId), any())).thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<List<MealPlanRowDTO>>> result = mealPlanController.addRows(sectionId, req);

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
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, UUID.randomUUID(), new BigDecimal("100.00"));
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(
                rowId, MealType.LUNCH, req.foodId(), "Haferflocken",
                new BigDecimal("100.00"), new BigDecimal("577.00"), new BigDecimal("31.00"),
                new BigDecimal("66.00"), new BigDecimal("11.00"));
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
    @DisplayName("updateRow returns 404 when the row or referenced food does not exist")
    void updateRow_NotFound_Returns404() {
        final UUID rowId = UUID.randomUUID();
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, UUID.randomUUID(), new BigDecimal("100.00"));
        when(mealPlanWriteService.updateRow(eq(rowId), any(UpdateMealPlanRowRequest.class)))
                .thenThrow(new NoSuchElementException("not found"));

        final Mono<ResponseEntity<MealPlanRowDTO>> result = mealPlanController.updateRow(rowId, req);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // DELETE /nutrition/meal-plan/rows/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteRow returns 204 with no body")
    void deleteRow_Returns204() {
        final UUID rowId = UUID.randomUUID();

        final Mono<ResponseEntity<Void>> result = mealPlanController.deleteRow(rowId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCode().value());
                    assertNull(response.getBody());
                })
                .verifyComplete();
        verify(mealPlanWriteService).deleteRow(rowId);
    }

    @Test
    @DisplayName("deleteRow returns 404 when the row does not exist")
    void deleteRow_NotFound_Returns404() {
        final UUID rowId = UUID.randomUUID();
        doThrow(new NoSuchElementException("not found")).when(mealPlanWriteService).deleteRow(rowId);

        final Mono<ResponseEntity<Void>> result = mealPlanController.deleteRow(rowId);

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
    // DELETE /nutrition/meal-plan/sources/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteSource returns 204 with no body")
    void deleteSource_Returns204() {
        final UUID sourceId = UUID.randomUUID();

        final Mono<ResponseEntity<Void>> result = mealPlanController.deleteSource(sourceId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCode().value());
                    assertNull(response.getBody());
                })
                .verifyComplete();
        verify(mealPlanWriteService).deleteSource(sourceId);
    }

    @Test
    @DisplayName("deleteSource returns 404 when the source does not exist")
    void deleteSource_NotFound_Returns404() {
        final UUID sourceId = UUID.randomUUID();
        doThrow(new NoSuchElementException("not found")).when(mealPlanWriteService).deleteSource(sourceId);

        final Mono<ResponseEntity<Void>> result = mealPlanController.deleteSource(sourceId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }
}
