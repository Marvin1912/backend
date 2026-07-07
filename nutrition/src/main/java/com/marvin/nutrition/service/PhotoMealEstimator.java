package com.marvin.nutrition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.nutrition.dto.MealEstimateDTO;
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
 * Estimates nutritional macros for a photographed canteen meal by calling the Anthropic Claude
 * Vision API. Returns a transient {@link MealEstimateDTO} — nothing is persisted.
 */
@Service
public class PhotoMealEstimator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoMealEstimator.class);
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 1024;
    private static final String PHOTO_ESTIMATE_PROMPT = """
            You are a nutritionist estimating macros for a canteen meal shown in a photo.
            Identify the food shown in the image and estimate its nutritional values.
            Return ONLY a strict JSON object with no markdown, no explanation, and no code block — just the raw JSON.
            Use exactly these keys: kcal, proteinG, carbsG, fatG, assumptions.
            The "assumptions" field must contain a concise English sentence describing your assumptions
            (e.g. what food was identified and the portion size used).
            Numbers must be plain decimals without units.""";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new PhotoMealEstimator.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param apiKey           the Anthropic API key (from {@code nutrition.claude.api-key})
     * @param objectMapper     Jackson mapper used to parse Claude's JSON response
     */
    public PhotoMealEstimator(
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
     * Sends the meal photo (and optional portion hint) to Claude and returns a parsed
     * {@link MealEstimateDTO}.
     * Emits {@link MealEstimateException} if Claude's response cannot be parsed as JSON,
     * if the response contains no content, or if the result lacks a usable {@code kcal} value.
     *
     * @param imageBytes  raw bytes of the meal photo (JPEG or PNG)
     * @param portionHint optional hint about the portion size; may be {@code null}
     * @return a Mono emitting the parsed macro estimate, or an error if estimation fails
     */
    public Mono<MealEstimateDTO> estimateFromPhoto(byte[] imageBytes, String portionHint) {
        LOGGER.info("Sending meal photo ({} bytes) to Claude Vision estimator API", imageBytes.length);
        final String base64 = Base64.getEncoder().encodeToString(imageBytes);
        final String mediaType = detectMediaType(imageBytes);
        final Map<String, Object> request = buildRequest(base64, mediaType, portionHint);

        return webClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(WebClientResponseException.class,
                        e -> new MealEstimateException("Claude API request failed: " + e.getStatusCode(), e))
                .flatMap(this::parseClaudeResponse)
                .doOnError(e -> LOGGER.error("Photo meal estimation failed", e));
    }

    @SuppressWarnings("unchecked")
    private Mono<MealEstimateDTO> parseClaudeResponse(Map<?, ?> response) {
        final List<?> content = (List<?>) response.get("content");
        if (content == null || content.isEmpty()) {
            return Mono.error(new MealEstimateException("Claude returned an empty content list"));
        }
        final Optional<String> text = findFirstTextBlock(content);
        if (text.isEmpty()) {
            return Mono.error(new MealEstimateException("Claude returned no text content block"));
        }
        return parseEstimate(text.get());
    }

    /**
     * Parses the given text as a {@link MealEstimateDTO} and validates that a usable kcal value is present.
     *
     * @param text the trimmed text content extracted from Claude's response
     * @return a Mono emitting the parsed estimate, or an error if parsing or validation fails
     */
    private Mono<MealEstimateDTO> parseEstimate(String text) {
        try {
            final MealEstimateDTO estimate = objectMapper.readValue(text, MealEstimateDTO.class);
            if (estimate.kcal() == null) {
                return Mono.error(new MealEstimateException("Claude returned no usable macro estimate"));
            }
            LOGGER.info("Parsed photo meal estimate: kcal={}", estimate.kcal());
            return Mono.just(estimate);
        } catch (Exception e) {
            LOGGER.warn("Claude returned non-JSON text: {}", text);
            return Mono.error(new MealEstimateException("Claude returned a non-JSON response: " + text, e));
        }
    }

    private Map<String, Object> buildRequest(String base64, String mediaType, String portionHint) {
        final StringBuilder promptText = new StringBuilder(PHOTO_ESTIMATE_PROMPT);
        if (portionHint != null && !portionHint.isBlank()) {
            promptText.append("\nPortion: ").append(portionHint);
        }
        final Map<String, Object> imageSource = Map.of(
                "type", "base64",
                "media_type", mediaType,
                "data", base64);
        final Map<String, Object> imageBlock = Map.of("type", "image", "source", imageSource);
        final Map<String, Object> textBlock = Map.of("type", "text", "text", promptText.toString());
        final Map<String, Object> message = Map.of("role", "user", "content", List.of(imageBlock, textBlock));
        return Map.of("model", MODEL, "max_tokens", MAX_TOKENS, "messages", List.of(message));
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
