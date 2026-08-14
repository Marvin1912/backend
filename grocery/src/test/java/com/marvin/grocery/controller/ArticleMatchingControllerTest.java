package com.marvin.grocery.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.ArticleDTO;
import com.marvin.grocery.dto.ArticleGroupSuggestionDTO;
import com.marvin.grocery.dto.MatchingRunResultDTO;
import com.marvin.grocery.entity.SuggestionSource;
import com.marvin.grocery.matching.ArticleGroupMatchingException;
import com.marvin.grocery.matching.ArticleGroupSuggestionService;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleMatchingController Tests")
class ArticleMatchingControllerTest {

    @Mock
    private ArticleGroupSuggestionService articleGroupSuggestionService;

    @InjectMocks
    private ArticleMatchingController controller;

    @Test
    @DisplayName("Should return all pending suggestions as a Flux")
    void listSuggestions_ReturnsFluxOfSuggestions() {
        final ArticleGroupSuggestionDTO dto =
                new ArticleGroupSuggestionDTO(1L, 2L, "Vollmilch", "vollmilch", 3L, "Dairy", 0.8, SuggestionSource.HEURISTIC);
        when(articleGroupSuggestionService.listPending()).thenReturn(List.of(dto));

        final Flux<ArticleGroupSuggestionDTO> result = controller.listSuggestions();

        StepVerifier.create(result)
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 200 with the updated article after accepting a suggestion")
    void acceptSuggestion_Pending_Returns200() {
        final ArticleDTO dto = new ArticleDTO(2L, "vollmilch", "Vollmilch", 3L, "Dairy", 5L);
        when(articleGroupSuggestionService.accept(1L)).thenReturn(dto);

        final Mono<ResponseEntity<ArticleDTO>> result = controller.acceptSuggestion(1L);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(2L, response.getBody().id());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 404 when accepting a suggestion that does not exist")
    void acceptSuggestion_NotFound_Returns404() {
        when(articleGroupSuggestionService.accept(eq(99L)))
                .thenThrow(new NoSuchElementException("Suggestion not found: 99"));

        final Mono<ResponseEntity<ArticleDTO>> result = controller.acceptSuggestion(99L);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 409 when accepting a suggestion that is not pending")
    void acceptSuggestion_NotPending_Returns409() {
        when(articleGroupSuggestionService.accept(eq(1L)))
                .thenThrow(new IllegalStateException("Suggestion is not pending: 1"));

        final Mono<ResponseEntity<ArticleDTO>> result = controller.acceptSuggestion(1L);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(409, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 204 after rejecting a pending suggestion")
    void rejectSuggestion_Pending_Returns204() {
        final Mono<ResponseEntity<Void>> result = controller.rejectSuggestion(1L);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 404 when rejecting a suggestion that does not exist")
    void rejectSuggestion_NotFound_Returns404() {
        doThrow(new NoSuchElementException("Suggestion not found: 99"))
                .when(articleGroupSuggestionService).reject(99L);

        final Mono<ResponseEntity<Void>> result = controller.rejectSuggestion(99L);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 409 when rejecting a suggestion that is not pending")
    void rejectSuggestion_NotPending_Returns409() {
        doThrow(new IllegalStateException("Suggestion is not pending: 1"))
                .when(articleGroupSuggestionService).reject(1L);

        final Mono<ResponseEntity<Void>> result = controller.rejectSuggestion(1L);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(409, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 200 with the run summary after a heuristic backfill run")
    void runHeuristicBackfill_Always_Returns200() {
        final MatchingRunResultDTO dto = new MatchingRunResultDTO(5, 2, 1, 2);
        when(articleGroupSuggestionService.runHeuristicBackfill()).thenReturn(dto);

        final Mono<ResponseEntity<MatchingRunResultDTO>> result = controller.runHeuristicBackfill();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(5, response.getBody().candidatesEvaluated());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 200 with the run summary after an LLM batch run")
    void runLlmBatch_Success_Returns200() {
        final MatchingRunResultDTO dto = new MatchingRunResultDTO(3, 2, 0, 1);
        when(articleGroupSuggestionService.runLlmBatch()).thenReturn(Mono.just(dto));

        final Mono<ResponseEntity<MatchingRunResultDTO>> result = controller.runLlmBatch();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(3, response.getBody().candidatesEvaluated());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 502 when the LLM batch run fails")
    void runLlmBatch_MatchingException_Returns502() {
        when(articleGroupSuggestionService.runLlmBatch())
                .thenReturn(Mono.error(new ArticleGroupMatchingException("Claude API request failed")));

        final Mono<ResponseEntity<MatchingRunResultDTO>> result = controller.runLlmBatch();

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(502, response.getStatusCode().value()))
                .verifyComplete();
    }
}
