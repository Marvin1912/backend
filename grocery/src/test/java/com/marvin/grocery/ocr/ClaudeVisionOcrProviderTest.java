package com.marvin.grocery.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marvin.grocery.ocr.ClaudeVisionOcrProvider.ClaudeResponse;
import com.marvin.grocery.ocr.ClaudeVisionOcrProvider.ClaudeResponse.ContentBlock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("ClaudeVisionOcrProvider Tests")
class ClaudeVisionOcrProviderTest {

    private ClaudeVisionOcrProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ClaudeVisionOcrProvider(WebClient.builder(), "test-key");
    }

    @Test
    @DisplayName("Should return the text of the first content block")
    void extractTextFromResponse_validResponse_returnsText() {
        final ClaudeResponse response =
                new ClaudeResponse("end_turn", List.of(new ContentBlock("Milch  0.99  1  0.99")));

        assertEquals("Milch  0.99  1  0.99", provider.extractTextFromResponse(response));
    }

    @Test
    @DisplayName("Should still return text when the response was truncated")
    void extractTextFromResponse_truncatedResponse_returnsText() {
        final ClaudeResponse response =
                new ClaudeResponse("max_tokens", List.of(new ContentBlock("Milch  0.99  1  0.99")));

        assertEquals("Milch  0.99  1  0.99", provider.extractTextFromResponse(response));
    }

    @Test
    @DisplayName("Should throw when the content list is empty")
    void extractTextFromResponse_emptyContent_throws() {
        final ClaudeResponse response = new ClaudeResponse("end_turn", List.of());

        assertThrows(OcrExtractionException.class, () -> provider.extractTextFromResponse(response));
    }

    @Test
    @DisplayName("Should throw when the content list is null")
    void extractTextFromResponse_nullContent_throws() {
        final ClaudeResponse response = new ClaudeResponse("end_turn", null);

        assertThrows(OcrExtractionException.class, () -> provider.extractTextFromResponse(response));
    }

    @Test
    @DisplayName("Should throw when the first content block has no text")
    void extractTextFromResponse_nullText_throws() {
        final ClaudeResponse response =
                new ClaudeResponse("end_turn", List.of(new ContentBlock(null)));

        assertThrows(OcrExtractionException.class, () -> provider.extractTextFromResponse(response));
    }

    @Test
    @DisplayName("Should throw when the first element of the content list is null (e.g. JSON [null])")
    void extractTextFromResponse_nullFirstElement_throws() {
        final List<ContentBlock> contentWithNullElement = new java.util.ArrayList<>();
        contentWithNullElement.add(null);
        final ClaudeResponse response = new ClaudeResponse("end_turn", contentWithNullElement);

        assertThrows(OcrExtractionException.class, () -> provider.extractTextFromResponse(response));
    }
}
