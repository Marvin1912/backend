package com.marvin.grocery.matching;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Matches ungrouped {@link ArticleEntity} rows to existing {@link ArticleGroupEntity} rows by asking
 * the Anthropic Claude API to reason about German grocery product names in a single batched call.
 */
@Service
public class ArticleGroupLlmMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleGroupLlmMatcher.class);
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 4096;
    private static final String PROMPT_HEADER = """
            You are matching German grocery articles to existing product groups.
            Return ONLY a strict JSON object with no markdown, no explanation, and no code block — just the raw JSON.
            Use exactly this shape: {"matches": [{"articleId": <id>, "groupId": <id>, "confidence": <0..1>}]}.
            Only include matches you are confident about; omit any article you are not sure about instead of guessing.
            Do not invent article ids or group ids that are not listed below.""";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new ArticleGroupLlmMatcher.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param apiKey           the Anthropic API key (from {@code grocery.claude.api-key})
     * @param objectMapper     Jackson mapper used to parse Claude's JSON response
     */
    public ArticleGroupLlmMatcher(
            WebClient.Builder webClientBuilder,
            @Value("${grocery.claude.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Asks Claude to match every candidate article to one of the existing groups, dropping any
     * match that hallucinates an article or group id that was not part of the request.
     *
     * @param candidateArticles the ungrouped articles to attempt to match
     * @param existingGroups    the existing article groups Claude may match against
     * @return a Mono emitting the confirmed (non-hallucinated) matches
     */
    public Mono<List<LlmMatchResponse.Match>> matchBatch(
            List<ArticleEntity> candidateArticles, List<ArticleGroupEntity> existingGroups) {
        LOGGER.info("Sending {} candidate articles and {} groups to Claude for group matching",
                candidateArticles.size(), existingGroups.size());
        final Map<String, Object> request = buildRequest(candidateArticles, existingGroups);
        final Set<Long> articleIds = candidateArticles.stream().map(ArticleEntity::getId).collect(Collectors.toSet());
        final Set<Long> groupIds = existingGroups.stream().map(ArticleGroupEntity::getId).collect(Collectors.toSet());

        return webClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(WebClientResponseException.class,
                        e -> new ArticleGroupMatchingException("Claude API request failed: " + e.getStatusCode(), e))
                .flatMap(this::parseClaudeResponse)
                .map(matches -> filterHallucinations(matches, articleIds, groupIds))
                .doOnError(e -> LOGGER.error("Article group LLM matching failed", e));
    }

    @SuppressWarnings("unchecked")
    private Mono<List<LlmMatchResponse.Match>> parseClaudeResponse(Map<?, ?> response) {
        final List<?> content = (List<?>) response.get("content");
        if (content == null || content.isEmpty()) {
            return Mono.error(new ArticleGroupMatchingException("Claude returned an empty content list"));
        }
        final Optional<String> text = findFirstTextBlock(content);
        if (text.isEmpty()) {
            return Mono.error(new ArticleGroupMatchingException("Claude returned no text content block"));
        }
        return parseMatches(text.get());
    }

    private Mono<List<LlmMatchResponse.Match>> parseMatches(String text) {
        try {
            final LlmMatchResponse parsed = objectMapper.readValue(text, LlmMatchResponse.class);
            final List<LlmMatchResponse.Match> matches = parsed.matches() == null ? List.of() : parsed.matches();
            LOGGER.info("Parsed {} candidate matches from Claude", matches.size());
            return Mono.just(matches);
        } catch (Exception e) {
            LOGGER.warn("Claude returned non-JSON text: {}", text);
            return Mono.error(new ArticleGroupMatchingException("Claude returned a non-JSON response: " + text, e));
        }
    }

    private List<LlmMatchResponse.Match> filterHallucinations(
            List<LlmMatchResponse.Match> matches, Set<Long> articleIds, Set<Long> groupIds) {
        return matches.stream()
                .filter(match -> isValidMatch(match, articleIds, groupIds))
                .toList();
    }

    private boolean isValidMatch(LlmMatchResponse.Match match, Set<Long> articleIds, Set<Long> groupIds) {
        final boolean valid = articleIds.contains(match.articleId()) && groupIds.contains(match.groupId());
        if (!valid) {
            LOGGER.warn("Dropping hallucinated LLM match: articleId={}, groupId={}", match.articleId(), match.groupId());
        }
        return valid;
    }

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

    private Map<String, Object> buildRequest(List<ArticleEntity> candidateArticles, List<ArticleGroupEntity> existingGroups) {
        final StringBuilder promptText = new StringBuilder(PROMPT_HEADER);
        promptText.append("\n\nExisting groups (id: name):\n");
        for (final ArticleGroupEntity group : existingGroups) {
            promptText.append(group.getId()).append(": ").append(group.getName()).append('\n');
        }
        promptText.append("\nUngrouped articles (id: name):\n");
        for (final ArticleEntity article : candidateArticles) {
            promptText.append(article.getId()).append(": ").append(article.getName()).append('\n');
        }
        final Map<String, Object> textBlock = Map.of("type", "text", "text", promptText.toString());
        final Map<String, Object> message = Map.of("role", "user", "content", List.of(textBlock));
        return Map.of("model", MODEL, "max_tokens", MAX_TOKENS, "messages", List.of(message));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmMatchResponse(List<Match> matches) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Match(
                @JsonProperty("articleId") Long articleId,
                @JsonProperty("groupId") Long groupId,
                @JsonProperty("confidence") Double confidence) {
        }
    }
}
