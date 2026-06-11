package com.marvin.nutrition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.nutrition.dto.FoodDraftDTO;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Reads nutritional values from a packaged food label photo by calling the Anthropic Claude API.
 * Returns a transient {@link FoodDraftDTO} — nothing is persisted.
 */
@Service
public class LabelReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LabelReader.class);
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 1024;
    private static final String LABEL_PROMPT = """
            You are a nutrition label reader. Analyse the food packaging image and return ONLY a strict JSON object.
            No markdown, no explanation, no code block — just the raw JSON.
            Use exactly these keys: name, brand, kcalPer100, proteinPer100, carbsPer100, fatPer100, fiberPer100, servingG.
            All macronutrient values must be normalised to per 100 g (convert from per-serving if necessary).
            Use null for any field that is not visible on the label.
            Numbers must be plain decimals without units.""";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new LabelReader.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param apiKey           the Anthropic API key (from {@code nutrition.claude.api-key})
     * @param objectMapper     Jackson mapper used to parse Claude's JSON response
     */
    public LabelReader(
            WebClient.Builder webClientBuilder,
            @Value("${nutrition.claude.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the image bytes to Claude and returns a parsed {@link FoodDraftDTO}.
     * Emits {@link LabelReadException} if Claude's response cannot be parsed as JSON
     * or if the response contains no content.
     *
     * @param imageBytes raw bytes of the label image (JPEG or PNG)
     * @return a Mono emitting the parsed draft food, or an error if parsing fails
     */
    public Mono<FoodDraftDTO> readLabel(byte[] imageBytes) {
        LOGGER.info("Sending nutrition label image ({} bytes) to Claude Vision API", imageBytes.length);
        final String base64 = Base64.getEncoder().encodeToString(imageBytes);
        final String mediaType = detectMediaType(imageBytes);
        final Map<String, Object> request = buildRequest(base64, mediaType);

        return webClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(WebClientResponseException.class,
                        e -> new LabelReadException("Claude API request failed: " + e.getStatusCode(), e))
                .flatMap(this::parseClaudeResponse)
                .doOnError(e -> LOGGER.error("Label read failed", e));
    }

    @SuppressWarnings("unchecked")
    private Mono<FoodDraftDTO> parseClaudeResponse(Map<?, ?> response) {
        final List<?> content = (List<?>) response.get("content");
        if (content == null || content.isEmpty()) {
            return Mono.error(new LabelReadException("Claude returned an empty content list"));
        }
        final Optional<String> text = findFirstTextBlock(content);
        if (text.isEmpty()) {
            return Mono.error(new LabelReadException("Claude returned no text content block"));
        }
        return parseDraft(text.get());
    }

    /**
     * Parses the given text as a {@link FoodDraftDTO} and validates that usable nutrition data is present.
     *
     * @param text the trimmed text content extracted from Claude's response
     * @return a Mono emitting the parsed draft, or an error if parsing or validation fails
     */
    private Mono<FoodDraftDTO> parseDraft(String text) {
        try {
            final FoodDraftDTO draft = objectMapper.readValue(text, FoodDraftDTO.class);
            if (draft.name() == null || draft.name().isBlank() || draft.kcalPer100() == null) {
                return Mono.error(new LabelReadException("Claude returned no usable nutrition data"));
            }
            LOGGER.info("Parsed nutrition label draft: name={}", draft.name());
            return Mono.just(draft);
        } catch (Exception e) {
            LOGGER.warn("Claude returned non-JSON text: {}", text);
            return Mono.error(new LabelReadException("Claude returned a non-JSON response: " + text, e));
        }
    }

    /**
     * Finds the first content block whose {@code type} is {@code "text"} and returns its trimmed {@code text}.
     * Blocks that are not maps, or whose type is not {@code "text"}, are skipped.
     *
     * @param content the list of content blocks returned by Claude
     * @return the trimmed text of the first text block, or empty if none was found
     */
    private Optional<String> findFirstTextBlock(List<?> content) {
        for (final Object blockObj : content) {
            if (!(blockObj instanceof Map<?, ?> block)) {
                continue;
            }
            if ("text".equals(String.valueOf(block.get("type")))) {
                final Object textObj = block.get("text");
                final String text = textObj != null ? textObj.toString().trim() : "";
                return Optional.of(text);
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> buildRequest(String base64, String mediaType) {
        final Map<String, Object> imageSource = Map.of(
                "type", "base64",
                "media_type", mediaType,
                "data", base64);
        final Map<String, Object> imageBlock = Map.of("type", "image", "source", imageSource);
        final Map<String, Object> textBlock = Map.of("type", "text", "text", LABEL_PROMPT);
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
}
