package com.marvin.itnews.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.itnews.dto.FeedSourceDTO;
import com.marvin.itnews.service.FeedConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies that Bean Validation failures on {@code @Valid @RequestBody} arguments are routed
 * through {@link ItNewsExceptionHandler#handleValidation} and produce the field-level error
 * message, rather than falling through to the WebFlux default error body.
 *
 * <p>Spring WebFlux annotated controllers raise {@code WebExchangeBindException} for
 * {@code @Valid @RequestBody} failures, not the Spring MVC {@code MethodArgumentNotValidException}
 * that the handler was originally written against.</p>
 */
@WebFluxTest(
        controllers = FeedConfigController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@DisplayName("FeedConfigController request body validation Tests")
class FeedConfigControllerBodyValidationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FeedConfigService feedConfigService;

    @Test
    @DisplayName("POST /it-news/feeds/ with a blank name returns 400 with the field-level error message")
    void createFeedConfig_BlankName_ReturnsFieldLevelValidationMessage() {
        final FeedSourceDTO invalid = new FeedSourceDTO(null, "", "https://example.com/rss", "Java", true);

        final String body = webTestClient.post()
                .uri("/it-news/feeds/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalid)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).contains("name");
    }
}
