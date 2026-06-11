package com.marvin.nutrition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.service.BarcodeLookup;
import com.marvin.nutrition.service.FoodService;
import com.marvin.nutrition.service.LabelReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * End-to-end validation tests for the pagination parameters on {@code GET /nutrition/foods}.
 * Exercises the real {@code @Validated} method-validation proxy and the
 * {@link NutritionExceptionHandler} constraint-violation mapping via {@link WebTestClient}.
 */
@WebFluxTest(
        controllers = FoodController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@DisplayName("FoodController pagination validation Tests")
class FoodControllerValidationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FoodService foodService;

    @MockitoBean
    private LabelReader labelReader;

    @MockitoBean
    private BarcodeLookup barcodeLookup;

    @Test
    @DisplayName("GET /nutrition/foods with default pagination returns 200")
    void listFoods_DefaultPagination_ReturnsOk() {
        when(foodService.findAll(any(), anyInt(), anyInt())).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/nutrition/foods")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /nutrition/foods with size above the maximum returns 400")
    void listFoods_SizeAboveMax_ReturnsBadRequest() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/nutrition/foods").queryParam("size", 201).build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /nutrition/foods with size below the minimum returns 400")
    void listFoods_SizeBelowMin_ReturnsBadRequest() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/nutrition/foods").queryParam("size", 0).build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /nutrition/foods with negative page returns 400")
    void listFoods_NegativePage_ReturnsBadRequest() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/nutrition/foods").queryParam("page", -1).build())
                .exchange()
                .expectStatus().isBadRequest();
    }
}
