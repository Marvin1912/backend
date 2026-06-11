package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** Unit tests for {@link LabelReader} covering success and error paths. */
@ExtendWith(MockitoExtension.class)
@DisplayName("LabelReaderTest")
class LabelReaderTest {

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

    private LabelReader labelReader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Sets up the WebClient mock chain and creates the service under test. */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.baseUrl(any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(String.class), any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        labelReader = new LabelReader(webClientBuilder, "test-api-key", objectMapper);
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
    @DisplayName("readLabel returns parsed FoodDraftDTO for valid JSON response")
    void readLabel_ValidJson_ReturnsParsedDTO() {
        final String jsonText = """
                {
                    "name": "Müsli",
                    "brand": "Kellogg's",
                    "kcalPer100": 370.0,
                    "proteinPer100": 8.5,
                    "carbsPer100": 67.0,
                    "fatPer100": 6.0,
                    "fiberPer100": 5.5,
                    "servingG": 45.0
                }""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        StepVerifier.create(labelReader.readLabel(pngBytes))
                .assertNext(dto -> {
                    assertEquals("Müsli", dto.name());
                    assertEquals("Kellogg's", dto.brand());
                    assertEquals(0, new BigDecimal("370.0").compareTo(dto.kcalPer100()));
                    assertEquals(0, new BigDecimal("8.5").compareTo(dto.proteinPer100()));
                    assertEquals(0, new BigDecimal("67.0").compareTo(dto.carbsPer100()));
                    assertEquals(0, new BigDecimal("6.0").compareTo(dto.fatPer100()));
                    assertEquals(0, new BigDecimal("5.5").compareTo(dto.fiberPer100()));
                    assertEquals(0, new BigDecimal("45.0").compareTo(dto.servingG()));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("readLabel returns FoodDraftDTO with null optional fields when Claude returns nulls")
    void readLabel_ValidJsonWithNulls_ReturnsDTOWithNullOptionals() {
        final String jsonText = """
                {
                    "name": "Plain Rice",
                    "brand": null,
                    "kcalPer100": 130.0,
                    "proteinPer100": 2.7,
                    "carbsPer100": 28.0,
                    "fatPer100": 0.3,
                    "fiberPer100": null,
                    "servingG": null
                }""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8};
        StepVerifier.create(labelReader.readLabel(jpegBytes))
                .assertNext(dto -> {
                    assertEquals("Plain Rice", dto.name());
                    assertNull(dto.brand());
                    assertNull(dto.fiberPer100());
                    assertNull(dto.servingG());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("readLabel emits LabelReadException when Claude returns non-JSON garbage text")
    void readLabel_GarbageText_EmitsLabelReadException() {
        final String garbageText = "Sorry, I cannot read the label in this image.";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", garbageText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] imageBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        StepVerifier.create(labelReader.readLabel(imageBytes))
                .expectError(LabelReadException.class)
                .verify();
    }

    @Test
    @DisplayName("readLabel emits LabelReadException when Claude response has empty content list")
    void readLabel_EmptyContent_EmitsLabelReadException() {
        final Map<String, Object> claudeResponse = Map.of("content", List.of());
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] imageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8};
        StepVerifier.create(labelReader.readLabel(imageBytes))
                .expectError(LabelReadException.class)
                .verify();
    }

    @Test
    @DisplayName("readLabel emits LabelReadException when Claude API returns HTTP 401")
    void readLabel_ApiReturns401_EmitsLabelReadException() {
        final WebClientResponseException unauthorizedException = WebClientResponseException.create(
                401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
        stubWebClientChain(Mono.error(unauthorizedException));

        final byte[] imageBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        StepVerifier.create(labelReader.readLabel(imageBytes))
                .expectError(LabelReadException.class)
                .verify();
    }

    @Test
    @DisplayName("readLabel emits LabelReadException when Claude returns valid JSON with no usable fields")
    void readLabel_EmptyJsonObject_EmitsLabelReadException() {
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", "{}"))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] imageBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        StepVerifier.create(labelReader.readLabel(imageBytes))
                .expectError(LabelReadException.class)
                .verify();
    }

    @Test
    @DisplayName("readLabel returns parsed FoodDraftDTO when a non-text block precedes the text block")
    void readLabel_LeadingNonTextBlock_ReturnsParsedDTO() {
        final String jsonText = """
                {
                    "name": "Haferflocken",
                    "brand": "Bio",
                    "kcalPer100": 350.0,
                    "proteinPer100": 12.0,
                    "carbsPer100": 60.0,
                    "fatPer100": 7.0,
                    "fiberPer100": 10.0,
                    "servingG": 50.0
                }""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(
                        Map.of("type", "thinking", "thinking", "Let me look at this label..."),
                        Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        StepVerifier.create(labelReader.readLabel(pngBytes))
                .assertNext(dto -> {
                    assertEquals("Haferflocken", dto.name());
                    assertEquals(0, new BigDecimal("350.0").compareTo(dto.kcalPer100()));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("readLabel emits LabelReadException when no content block has type text")
    void readLabel_NoTextBlock_EmitsLabelReadException() {
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "thinking", "thinking", "Hmm..."))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] imageBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        StepVerifier.create(labelReader.readLabel(imageBytes))
                .expectError(LabelReadException.class)
                .verify();
    }
}
