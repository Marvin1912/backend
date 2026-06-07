package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealEstimateDTO;
import com.marvin.nutrition.dto.MealEstimateRequest;
import com.marvin.nutrition.service.MealEstimateException;
import com.marvin.nutrition.service.MealEstimator;
import java.math.BigDecimal;
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

/** Unit tests for {@link MealEstimateController} covering success and error paths. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealEstimateController Tests")
class MealEstimateControllerTest {

    @Mock
    private MealEstimator mealEstimator;

    @InjectMocks
    private MealEstimateController mealEstimateController;

    private MealEstimateDTO estimateDTO;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        estimateDTO = new MealEstimateDTO(
                new BigDecimal("650.00"),
                new BigDecimal("45.00"),
                new BigDecimal("70.00"),
                new BigDecimal("18.00"),
                "Estimated for a standard canteen portion of 400 g"
        );
    }

    @Test
    @DisplayName("estimate returns 200 with MealEstimateDTO when estimator succeeds without portionHint")
    void estimate_Valid_NoPortionHint_Returns200WithDTO() {
        final MealEstimateRequest request = new MealEstimateRequest("Schnitzel mit Pommes", null);
        when(mealEstimator.estimate(eq("Schnitzel mit Pommes"), isNull()))
                .thenReturn(Mono.just(estimateDTO));

        final Mono<ResponseEntity<MealEstimateDTO>> result = mealEstimateController.estimate(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(0, new BigDecimal("650.00").compareTo(response.getBody().kcal()));
                    assertEquals(0, new BigDecimal("45.00").compareTo(response.getBody().proteinG()));
                    assertEquals(0, new BigDecimal("70.00").compareTo(response.getBody().carbsG()));
                    assertEquals(0, new BigDecimal("18.00").compareTo(response.getBody().fatG()));
                    assertEquals("Estimated for a standard canteen portion of 400 g",
                            response.getBody().assumptions());
                })
                .verifyComplete();

        verify(mealEstimator).estimate(eq("Schnitzel mit Pommes"), isNull());
    }

    @Test
    @DisplayName("estimate returns 200 with MealEstimateDTO when estimator succeeds with portionHint")
    void estimate_Valid_WithPortionHint_Returns200WithDTO() {
        final MealEstimateRequest request = new MealEstimateRequest("Spaghetti Bolognese", "one plate");
        when(mealEstimator.estimate(eq("Spaghetti Bolognese"), eq("one plate")))
                .thenReturn(Mono.just(estimateDTO));

        final Mono<ResponseEntity<MealEstimateDTO>> result = mealEstimateController.estimate(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                })
                .verifyComplete();

        verify(mealEstimator).estimate(eq("Spaghetti Bolognese"), eq("one plate"));
    }

    @Test
    @DisplayName("estimate propagates MealEstimateException when estimator fails")
    void estimate_EstimatorFails_PropagatesMealEstimateException() {
        final MealEstimateRequest request = new MealEstimateRequest("mystery food", null);
        when(mealEstimator.estimate(any(String.class), isNull()))
                .thenReturn(Mono.error(new MealEstimateException("Claude returned a non-JSON response")));

        final Mono<ResponseEntity<MealEstimateDTO>> result = mealEstimateController.estimate(request);

        StepVerifier.create(result)
                .expectError(MealEstimateException.class)
                .verify();

        verify(mealEstimator).estimate(any(String.class), isNull());
    }
}
