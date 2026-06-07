package com.marvin.nutrition.service;

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
import org.springframework.web.reactive.function.client.WebClient;
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
                "content", List.of(Map.of("text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        StepVerifier.create(labelReader.readLabel(pngBytes))
                .assertNext(dto -> {
                    assert "Müsli".equals(dto.name());
                    assert "Kellogg's".equals(dto.brand());
                    assert new BigDecimal("370.0").compareTo(dto.kcalPer100()) == 0;
                    assert new BigDecimal("8.5").compareTo(dto.proteinPer100()) == 0;
                    assert new BigDecimal("67.0").compareTo(dto.carbsPer100()) == 0;
                    assert new BigDecimal("6.0").compareTo(dto.fatPer100()) == 0;
                    assert new BigDecimal("5.5").compareTo(dto.fiberPer100()) == 0;
                    assert new BigDecimal("45.0").compareTo(dto.servingG()) == 0;
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
                "content", List.of(Map.of("text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        final byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8};
        StepVerifier.create(labelReader.readLabel(jpegBytes))
                .assertNext(dto -> {
                    assert "Plain Rice".equals(dto.name());
                    assert dto.brand() == null;
                    assert dto.fiberPer100() == null;
                    assert dto.servingG() == null;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("readLabel emits LabelReadException when Claude returns non-JSON garbage text")
    void readLabel_GarbageText_EmitsLabelReadException() {
        final String garbageText = "Sorry, I cannot read the label in this image.";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("text", garbageText))
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
}
