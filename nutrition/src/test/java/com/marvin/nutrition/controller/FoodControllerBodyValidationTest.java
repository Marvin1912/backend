package com.marvin.nutrition.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.service.BarcodeLookup;
import com.marvin.nutrition.service.FoodService;
import com.marvin.nutrition.service.LabelReader;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies that Bean Validation failures on {@code @Valid @RequestBody} arguments are routed
 * through {@link NutritionExceptionHandler#handleValidation} and produce the field-level error
 * message, rather than falling through to the WebFlux default error body.
 *
 * <p>Spring WebFlux annotated controllers raise {@code WebExchangeBindException} for
 * {@code @Valid @RequestBody} failures, not the Spring MVC {@code MethodArgumentNotValidException}
 * that the handler was originally written against.</p>
 */
@WebFluxTest(
        controllers = FoodController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@DisplayName("FoodController request body validation Tests")
class FoodControllerBodyValidationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FoodService foodService;

    @MockitoBean
    private LabelReader labelReader;

    @MockitoBean
    private BarcodeLookup barcodeLookup;

    @Test
    @DisplayName("POST /nutrition/foods with a blank name returns 400 with the field-level error message")
    void createFood_BlankName_ReturnsFieldLevelValidationMessage() {
        final FoodDTO invalid = new FoodDTO(
                null,
                "",
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                FoodSource.MANUAL
        );

        final String body = webTestClient.post()
                .uri("/nutrition/foods")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalid)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).contains("name");
    }
}
