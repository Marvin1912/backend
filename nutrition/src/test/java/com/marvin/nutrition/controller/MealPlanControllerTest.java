package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.dto.MealPlanFooterDTO;
import com.marvin.nutrition.dto.MealPlanShoppingListDTO;
import com.marvin.nutrition.service.MealPlanService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link MealPlanController} covering the read-only meal-plan endpoint. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanController Tests")
class MealPlanControllerTest {

    @Mock
    private MealPlanService mealPlanService;

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
}
