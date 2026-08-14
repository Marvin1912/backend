package com.marvin.grocery.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
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

/** Unit tests for {@link ArticleGroupLlmMatcher} covering parsing, the hallucination guard, and error paths. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleGroupLlmMatcherTest")
class ArticleGroupLlmMatcherTest {

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

    private ArticleGroupLlmMatcher matcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ArticleEntity milkArticle;
    private ArticleEntity breadArticle;
    private ArticleGroupEntity dairyGroup;

    /** Sets up the WebClient mock chain, the service under test, and shared candidate fixtures. */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.baseUrl(any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(String.class), any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        matcher = new ArticleGroupLlmMatcher(webClientBuilder, "test-api-key", objectMapper);

        milkArticle = new ArticleEntity();
        milkArticle.setId(1L);
        milkArticle.setName("Vollmilch");
        milkArticle.setNormalizedName("vollmilch");

        breadArticle = new ArticleEntity();
        breadArticle.setId(2L);
        breadArticle.setName("Toastbrot");
        breadArticle.setNormalizedName("toastbrot");

        dairyGroup = new ArticleGroupEntity();
        dairyGroup.setId(10L);
        dairyGroup.setName("Dairy");
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
    @DisplayName("Should parse a valid JSON batch response into matches")
    void matchBatch_ValidJson_ReturnsParsedMatches() {
        final String jsonText = """
                {"matches": [{"articleId": 1, "groupId": 10, "confidence": 0.95}]}""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(matcher.matchBatch(List.of(milkArticle, breadArticle), List.of(dairyGroup)))
                .assertNext(matches -> {
                    assertEquals(1, matches.size());
                    assertEquals(1L, matches.get(0).articleId());
                    assertEquals(10L, matches.get(0).groupId());
                    assertEquals(0.95, matches.get(0).confidence());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should drop a match referencing an articleId or groupId not in the candidate sets")
    void matchBatch_HallucinatedMatch_IsDropped() {
        final String jsonText = """
                {"matches": [
                    {"articleId": 1, "groupId": 10, "confidence": 0.95},
                    {"articleId": 999, "groupId": 10, "confidence": 0.9},
                    {"articleId": 2, "groupId": 999, "confidence": 0.9}
                ]}""";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", jsonText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(matcher.matchBatch(List.of(milkArticle, breadArticle), List.of(dairyGroup)))
                .assertNext(matches -> {
                    assertEquals(1, matches.size());
                    assertEquals(1L, matches.get(0).articleId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should raise ArticleGroupMatchingException when Claude returns non-JSON text")
    void matchBatch_NonJsonResponse_RaisesException() {
        final String garbageText = "Sorry, I cannot match these articles.";
        final Map<String, Object> claudeResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", garbageText))
        );
        stubWebClientChain(Mono.just(claudeResponse));

        StepVerifier.create(matcher.matchBatch(List.of(milkArticle), List.of(dairyGroup)))
                .expectError(ArticleGroupMatchingException.class)
                .verify();
    }

    @Test
    @DisplayName("Should raise ArticleGroupMatchingException when the HTTP call fails")
    void matchBatch_HttpError_RaisesException() {
        final WebClientResponseException unauthorized = WebClientResponseException.create(
                401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
        stubWebClientChain(Mono.error(unauthorized));

        StepVerifier.create(matcher.matchBatch(List.of(milkArticle), List.of(dairyGroup)))
                .expectError(ArticleGroupMatchingException.class)
                .verify();
    }
}
