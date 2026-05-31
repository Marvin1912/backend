package com.marvin.grocery.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/** Claude Vision-backed OCR provider. Sends the receipt image to the Anthropic API for robust text extraction. */
@Service
@Primary
public class ClaudeVisionOcrProvider implements OcrProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeVisionOcrProvider.class);
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 1024;
    private static final String RECEIPT_PROMPT = """
            Extract all purchased items from this German grocery receipt.
            For each item output it on its own line as: item name  price  quantity  total price
            If quantity is not available, use value 1 instead.
            Use exactly two spaces between the information of an item.
            Format prices with a period as decimal separator (e.g. 1.09).
            Include the receipt date on its own line in DD.MM.YYYY format if visible.
            Skip totals, taxes, payment method lines, and store header information.
            Output only the extracted lines, nothing else.""";

    private final WebClient webClient;

    /**
     * Creates a new ClaudeVisionOcrProvider.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param apiKey           the Anthropic API key (from {@code grocery.claude.api-key})
     */
    public ClaudeVisionOcrProvider(
            WebClient.Builder webClientBuilder,
            @Value("${grocery.claude.api-key}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public Mono<String> extractText(byte[] imageBytes) {
        LOGGER.info("Sending receipt image ({} bytes) to Claude Vision API", imageBytes.length);
        final String base64 = Base64.getEncoder().encodeToString(imageBytes);
        final String mediaType = detectMediaType(imageBytes);
        final Map<String, Object> request = buildRequest(base64, mediaType);

        return webClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .map(response -> {
                    final String text = response.content().get(0).text();
                    LOGGER.info("Claude Vision extracted {} characters", text.length());
                    return text;
                })
                .doOnError(e -> LOGGER.error("Claude Vision API call failed", e));
    }

    private Map<String, Object> buildRequest(String base64, String mediaType) {
        final Map<String, Object> imageSource = Map.of(
                "type", "base64",
                "media_type", mediaType,
                "data", base64);
        final Map<String, Object> imageBlock = Map.of("type", "image", "source", imageSource);
        final Map<String, Object> textBlock = Map.of("type", "text", "text", RECEIPT_PROMPT);
        final Map<String, Object> message = Map.of("role", "user", "content", List.of(imageBlock, textBlock));
        return Map.of("model", MODEL, "max_tokens", MAX_TOKENS, "messages", List.of(message));
    }

    private String detectMediaType(byte[] bytes) {
        if (bytes.length >= 4 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        return "image/jpeg";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClaudeResponse(@JsonProperty("content") List<ContentBlock> content) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ContentBlock(@JsonProperty("text") String text) { }
    }
}
