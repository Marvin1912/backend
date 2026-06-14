package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealTemplateRequest;
import com.marvin.nutrition.dto.MealTemplateDTO;
import com.marvin.nutrition.dto.MealTemplateItemDTO;
import com.marvin.nutrition.dto.MealTemplateItemRequest;
import com.marvin.nutrition.dto.UpdateMealTemplateRequest;
import com.marvin.nutrition.service.MealTemplateService;
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

/** Unit tests for {@link MealTemplateController} covering all endpoints. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealTemplateController Tests")
class MealTemplateControllerTest {

    @Mock
    private MealTemplateService mealTemplateService;

    @InjectMocks
    private MealTemplateController mealTemplateController;

    private UUID templateId;
    private UUID foodId;
    private MealTemplateDTO templateDTO;
    private CreateMealTemplateRequest createRequest;
    private UpdateMealTemplateRequest updateRequest;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        foodId = UUID.randomUUID();

        final MealTemplateItemDTO itemDTO = new MealTemplateItemDTO(
                UUID.randomUUID(), foodId, "Oatmeal", new BigDecimal("50"),
                new BigDecimal("185.00"), new BigDecimal("6.50"),
                new BigDecimal("30.00"), new BigDecimal("3.50")
        );

        templateDTO = new MealTemplateDTO(templateId, "Breakfast Bowl", List.of(itemDTO));

        createRequest = new CreateMealTemplateRequest(
                "Breakfast Bowl", List.of(new MealTemplateItemRequest(foodId, new BigDecimal("50")))
        );

        updateRequest = new UpdateMealTemplateRequest(
                "Renamed Bowl", List.of(new MealTemplateItemRequest(foodId, new BigDecimal("80")))
        );
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/meal-templates
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("listTemplates returns 200 with all templates")
    void listTemplates_Returns200WithAllTemplates() {
        when(mealTemplateService.findAll()).thenReturn(Mono.just(List.of(templateDTO)));

        final Mono<List<MealTemplateDTO>> result = mealTemplateController.listTemplates();

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(1, list.size());
                    assertEquals(templateId, list.get(0).id());
                })
                .verifyComplete();

        verify(mealTemplateService).findAll();
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/meal-templates/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTemplateById returns 200 with template when found")
    void getTemplateById_Found_Returns200() {
        when(mealTemplateService.findById(templateId)).thenReturn(Mono.just(templateDTO));

        final Mono<ResponseEntity<MealTemplateDTO>> result = mealTemplateController.getTemplateById(templateId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Breakfast Bowl", response.getBody().name());
                })
                .verifyComplete();

        verify(mealTemplateService).findById(templateId);
    }

    @Test
    @DisplayName("getTemplateById returns 404 when not found")
    void getTemplateById_NotFound_Returns404() {
        when(mealTemplateService.findById(templateId))
                .thenReturn(Mono.error(new NoSuchElementException("Meal template not found: " + templateId)));

        final Mono<ResponseEntity<MealTemplateDTO>> result = mealTemplateController.getTemplateById(templateId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealTemplateService).findById(templateId);
    }

    // -----------------------------------------------------------------------
    // POST /nutrition/meal-templates
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createTemplate returns 201 Created with Location header pointing to new template")
    void createTemplate_Valid_Returns201WithLocation() {
        when(mealTemplateService.create(any(CreateMealTemplateRequest.class))).thenReturn(Mono.just(templateDTO));

        final Mono<ResponseEntity<MealTemplateDTO>> result = mealTemplateController.createTemplate(createRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Breakfast Bowl", response.getBody().name());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(response.getHeaders().getLocation().toString().contains(templateId.toString()));
                })
                .verifyComplete();

        verify(mealTemplateService).create(eq(createRequest));
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/meal-templates/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateTemplate returns 200 with updated DTO when template exists")
    void updateTemplate_Found_Returns200WithUpdatedDTO() {
        final MealTemplateDTO updatedDTO = new MealTemplateDTO(templateId, "Renamed Bowl", templateDTO.items());

        when(mealTemplateService.update(eq(templateId), any(UpdateMealTemplateRequest.class)))
                .thenReturn(Mono.just(updatedDTO));

        final Mono<ResponseEntity<MealTemplateDTO>> result =
                mealTemplateController.updateTemplate(templateId, updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Renamed Bowl", response.getBody().name());
                })
                .verifyComplete();

        verify(mealTemplateService).update(eq(templateId), eq(updateRequest));
    }

    @Test
    @DisplayName("updateTemplate returns 404 when template not found")
    void updateTemplate_NotFound_Returns404() {
        when(mealTemplateService.update(eq(templateId), any(UpdateMealTemplateRequest.class)))
                .thenReturn(Mono.error(new NoSuchElementException("Meal template not found: " + templateId)));

        final Mono<ResponseEntity<MealTemplateDTO>> result =
                mealTemplateController.updateTemplate(templateId, updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealTemplateService).update(eq(templateId), eq(updateRequest));
    }

    // -----------------------------------------------------------------------
    // DELETE /nutrition/meal-templates/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteTemplate returns 204 No Content when template exists")
    void deleteTemplate_Exists_Returns204() {
        when(mealTemplateService.delete(templateId)).thenReturn(Mono.empty());

        final Mono<ResponseEntity<Void>> result = mealTemplateController.deleteTemplate(templateId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealTemplateService).delete(templateId);
    }

    @Test
    @DisplayName("deleteTemplate returns 404 when template not found")
    void deleteTemplate_NotFound_Returns404() {
        when(mealTemplateService.delete(templateId))
                .thenReturn(Mono.error(new NoSuchElementException("Meal template not found: " + templateId)));

        final Mono<ResponseEntity<Void>> result = mealTemplateController.deleteTemplate(templateId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealTemplateService).delete(templateId);
    }
}
