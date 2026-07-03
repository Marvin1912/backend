package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/** Unit tests for {@link MealPlanService} covering loading and parsing of the bundled meal-plan resource. */
@DisplayName("MealPlanService Tests")
class MealPlanServiceTest {

    private final MealPlanService mealPlanService = new MealPlanService(new ObjectMapper());

    // -----------------------------------------------------------------------
    // getMealPlan
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getMealPlan loads the bundled meal-plan resource with correct title")
    void getMealPlan_ReturnsMealPlanWithCorrectTitle() {
        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> assertEquals("Ernährungsplan & Einkaufsliste", dto.title()))
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns four stats with the expected labels and values")
    void getMealPlan_ReturnsFourStats() {
        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> {
                    assertEquals(4, dto.stats().size());
                    assertEquals("Tagesbudget (Ø)", dto.stats().get(0).label());
                    assertEquals("2.416 kcal", dto.stats().get(0).value());
                    assertEquals("Protein", dto.stats().get(1).label());
                    assertEquals("~184 g", dto.stats().get(1).value());
                    assertEquals("Kohlenhydrate", dto.stats().get(2).label());
                    assertEquals("~291 g", dto.stats().get(2).value());
                    assertEquals("Fett", dto.stats().get(3).label());
                    assertEquals("~52 g", dto.stats().get(3).value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns three sections")
    void getMealPlan_ReturnsThreeSections() {
        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> assertEquals(3, dto.sections().size()))
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns six shopping list categories")
    void getMealPlan_ReturnsSixShoppingCategories() {
        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> assertEquals(6, dto.shoppingList().categories().size()))
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns at least one shopping item with a non-null badge")
    void getMealPlan_ReturnsAtLeastOneItemWithBadge() {
        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> {
                    final boolean hasBadge = dto.shoppingList().categories().stream()
                            .flatMap(category -> category.items().stream())
                            .map(MealPlanShoppingItemDTO::badge)
                            .anyMatch(badge -> badge != null);
                    assertTrue(hasBadge);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns the same cached DTO instance on repeated calls")
    void getMealPlan_ReturnsCachedInstanceOnRepeatedCalls() {
        final MealPlanDTO first = mealPlanService.getMealPlan().block();
        final MealPlanDTO second = mealPlanService.getMealPlan().block();

        assertEquals(first, second);
    }
}
