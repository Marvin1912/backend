package com.marvin.nutrition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.nutrition.dto.MealEstimateDTO;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Estimates nutritional macros for a described canteen meal by calling the Anthropic Claude API
 * with a text-only prompt. Returns a transient {@link MealEstimateDTO} — nothing is persisted.
 */
@Service
public class MealEstimator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MealEstimator.class);
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 1024;
    private static final String ESTIMATE_PROMPT = """
            You are a nutritionist estimating macros for a canteen meal.
            Return ONLY a strict JSON object with no markdown, no explanation, and no code block — just the raw JSON.
            Use exactly these keys: kcal, proteinG, carbsG, fatG, assumptions.
            Estimate the values for the described meal and portion.
            The "assumptions" field must contain a concise English sentence describing your assumptions (e.g. portion size used).
            Numbers must be plain decimals without units.""";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new MealEstimator.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param apiKey           the Anthropic API key (from {@code nutrition.claude.api-key})
     * @param objectMapper     Jackson mapper used to parse Claude's JSON response
     */
    public MealEstimator(
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
     * Sends the meal description (and optional portion hint) to Claude and returns a parsed
     * {@link MealEstimateDTO}.
     * Emits {@link MealEstimateException} if Claude's response cannot be parsed as JSON,
     * if the response contains no content, or if the result lacks a usable {@code kcal} value.
     *
     * @param description free-text description of the meal
     * @param portionHint optional hint about the portion size; may be {@code null}
     * @return a Mono emitting the parsed macro estimate, or an error if estimation fails
     */
    public Mono<MealEstimateDTO> estimate(String description, String portionHint) {
        LOGGER.info("Sending meal description to Claude estimator API: {}", description);
        final Map<String, Object> request = buildRequest(description, portionHint);

        return webClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(WebClientResponseException.class,
                        e -> new MealEstimateException("Claude API request failed: " + e.getStatusCode(), e))
                .flatMap(this::parseClaudeResponse)
                .doOnError(e -> LOGGER.error("Meal estimation failed", e));
    }

    @SuppressWarnings("unchecked")
    private Mono<MealEstimateDTO> parseClaudeResponse(Map<?, ?> response) {
        final List<?> content = (List<?>) response.get("content");
        if (content == null || content.isEmpty()) {
            return Mono.error(new MealEstimateException("Claude returned an empty content list"));
        }
        final Map<?, ?> firstBlock = (Map<?, ?>) content.get(0);
        final Object textObj = firstBlock.get("text");
        final String text = textObj != null ? textObj.toString().trim() : "";
        try {
            final MealEstimateDTO estimate = objectMapper.readValue(text, MealEstimateDTO.class);
            if (estimate.kcal() == null) {
                return Mono.error(new MealEstimateException("Claude returned no usable macro estimate"));
            }
            LOGGER.info("Parsed meal estimate: kcal={}", estimate.kcal());
            return Mono.just(estimate);
        } catch (Exception e) {
            LOGGER.warn("Claude returned non-JSON text: {}", text);
            return Mono.error(new MealEstimateException("Claude returned a non-JSON response: " + text, e));
        }
    }

    private Map<String, Object> buildRequest(String description, String portionHint) {
        final StringBuilder promptText = new StringBuilder(ESTIMATE_PROMPT);
        promptText.append("\n\nMeal: ").append(description);
        if (portionHint != null && !portionHint.isBlank()) {
            promptText.append("\nPortion: ").append(portionHint);
        }
        final Map<String, Object> textBlock = Map.of("type", "text", "text", promptText.toString());
        final Map<String, Object> message = Map.of("role", "user", "content", List.of(textBlock));
        return Map.of("model", MODEL, "max_tokens", MAX_TOKENS, "messages", List.of(message));
    }
}
