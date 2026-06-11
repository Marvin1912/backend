package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link MealEstimator} covering success and error paths. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealEstimatorTest")
class MealEstimatorTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private MealEstimator mealEstimator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Sets up the WebClient mock chain and creates the service under test. */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.baseUrl(any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(String.class), any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        mealEstimator = new MealEstimator(webClientBuilder, "test-api-key", objectMapper);
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientChain(Mono<?> responseMono) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseMono).when(responseSpec).bodyToMono(any(Class.class));
    }

    @Test
    @DisplayName("estimate returns parsed MealEstimateDTO for valid JSON response without portionHint")
    void estimate_ValidJson_NoPortion_ReturnsParsedDTO() {
        final String jsonText = """
                {
                    "kcal": 650.0,
                    "proteinG": 45.0,
                    "carbsG": 70.0,
                    "fatG": 18.0,
                    "assumptions": "Estimated for a standard canteen portion of 400 g"
                }""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(mealEstimator.estimate("Schnitzel mit Pommes", null))
                .assertNext(dto -> {
                    assertEquals(0, new BigDecimal("650.0").compareTo(dto.kcal()));
                    assertEquals(0, new BigDecimal("45.0").compareTo(dto.proteinG()));
                    assertEquals(0, new BigDecimal("70.0").compareTo(dto.carbsG()));
                    assertEquals(0, new BigDecimal("18.0").compareTo(dto.fatG()));
                    assertEquals("Estimated for a standard canteen portion of 400 g", dto.assumptions());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("estimate returns parsed MealEstimateDTO for valid JSON response with portionHint")
    void estimate_ValidJson_WithPortionHint_ReturnsParsedDTO() {
        final String jsonText = """
                {
                    "kcal": 450.0,
                    "proteinG": 30.0,
                    "carbsG": 50.0,
                    "fatG": 12.0,
                    "assumptions": "Used the provided portion hint of one small plate"
                }""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(mealEstimator.estimate("Spaghetti Bolognese", "one small plate"))
                .assertNext(dto -> {
                    assertEquals(0, new BigDecimal("450.0").compareTo(dto.kcal()));
                    assertEquals(0, new BigDecimal("30.0").compareTo(dto.proteinG()));
                    assertEquals("Used the provided portion hint of one small plate", dto.assumptions());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("estimate emits MealEstimateException when Claude returns non-JSON garbage text")
    void estimate_GarbageText_EmitsMealEstimateException() {
        final String garbageText = "Sorry, I cannot estimate the meal from that description.";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", garbageText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(mealEstimator.estimate("unknown meal", null))
                .expectError(MealEstimateException.class)
                .verify();
    }

    @Test
    @DisplayName("estimate emits MealEstimateException when Claude response has empty content list")
    void estimate_EmptyContent_EmitsMealEstimateException() {
        final Map<String, Object> claudeResponse = Map.of("content", List.of());
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(mealEstimator.estimate("Pasta", null))
                .expectError(MealEstimateException.class)
                .verify();
    }

    @Test
    @DisplayName("estimate emits MealEstimateException when Claude API returns HTTP 401")
    void estimate_ApiReturns401_EmitsMealEstimateException() {
        final WebClientResponseException unauthorizedException = WebClientResponseException.create(
                401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
        stubWebClientChain(Mono.error(unauthorizedException));

        StepVerifier.create(mealEstimator.estimate("Currywurst", null))
                .expectError(MealEstimateException.class)
                .verify();
    }

    @Test
    @DisplayName("estimate emits MealEstimateException when Claude returns valid JSON with null kcal")
    void estimate_EmptyJsonObject_EmitsMealEstimateException() {
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", "{}"))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(mealEstimator.estimate("mystery dish", null))
                .expectError(MealEstimateException.class)
                .verify();
    }

    @Test
    @DisplayName("estimate returns parsed MealEstimateDTO when a non-text block precedes the text block")
    void estimate_LeadingNonTextBlock_ReturnsParsedDTO() {
        final String jsonText = """
                {
                    "kcal": 500.0,
                    "proteinG": 35.0,
                    "carbsG": 55.0,
                    "fatG": 15.0,
                    "assumptions": "Estimated for a standard portion"
                }""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(
                        Map.of("type", "thinking", "thinking", "Let me think about this..."),
                        Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(mealEstimator.estimate("Burger mit Pommes", null))
                .assertNext(dto -> {
                    assertEquals(0, new BigDecimal("500.0").compareTo(dto.kcal()));
                    assertEquals("Estimated for a standard portion", dto.assumptions());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("estimate emits MealEstimateException when no content block has type text")
    void estimate_NoTextBlock_EmitsMealEstimateException() {
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "thinking", "thinking", "Hmm..."))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(mealEstimator.estimate("mystery dish", null))
                .expectError(MealEstimateException.class)
                .verify();
    }
}
