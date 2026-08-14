package com.marvin.grocery.controller;

import com.marvin.grocery.dto.ArticleDTO;
import com.marvin.grocery.dto.ArticleGroupSuggestionDTO;
import com.marvin.grocery.dto.MatchingRunResultDTO;
import com.marvin.grocery.matching.ArticleGroupMatchingException;
import com.marvin.grocery.matching.ArticleGroupSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST controller exposing the automated article-to-group matching workflow: listing and deciding
 * on pending suggestions, and triggering heuristic or LLM-backed matching runs.
 */
@RestController("groceryArticleMatchingController")
@Tag(name = "Grocery Article Matching", description = "Automated heuristic and LLM-backed article-to-group matching")
public class ArticleMatchingController {

    private final ArticleGroupSuggestionService articleGroupSuggestionService;

    /**
     * Creates a new ArticleMatchingController with the required service.
     *
     * @param articleGroupSuggestionService the service handling automated article-to-group matching
     */
    public ArticleMatchingController(ArticleGroupSuggestionService articleGroupSuggestionService) {
        this.articleGroupSuggestionService = articleGroupSuggestionService;
    }

    /**
     * Lists all pending article-to-group suggestions, ordered by score descending.
     *
     * @return a Flux emitting all pending suggestion DTOs
     */
    @GetMapping("/articles/matching/suggestions")
    @Operation(
            summary = "List pending article-group suggestions",
            description = "Returns every suggestion currently awaiting a manual accept/reject decision, ordered by "
                    + "score descending.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Suggestion list returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArticleGroupSuggestionDTO.class)))
                )
            }
    )
    public Flux<ArticleGroupSuggestionDTO> listSuggestions() {
        return Flux.fromIterable(articleGroupSuggestionService.listPending());
    }

    /**
     * Accepts a pending suggestion, assigning its article to the suggested group.
     *
     * @param id the id of the suggestion to accept
     * @return a Mono with 200 OK and the updated article, 404 if not found, or 409 if not pending
     */
    @PostMapping("/articles/matching/suggestions/{id}/accept")
    @Operation(
            summary = "Accept a pending suggestion",
            description = "Assigns the suggestion's article to the suggested group and marks the suggestion accepted.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Suggestion accepted, article updated",
                        content = @Content(schema = @Schema(implementation = ArticleDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Suggestion not found"),
                @ApiResponse(responseCode = "409", description = "Suggestion is not pending")
            }
    )
    public Mono<ResponseEntity<ArticleDTO>> acceptSuggestion(
            @PathVariable @Parameter(description = "Id of the suggestion") Long id) {
        return Mono.fromCallable(() -> articleGroupSuggestionService.accept(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build())
                .onErrorReturn(IllegalStateException.class, ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    /**
     * Rejects a pending suggestion without touching its article's group assignment.
     *
     * @param id the id of the suggestion to reject
     * @return a Mono with 204 No Content, 404 if not found, or 409 if not pending
     */
    @PostMapping("/articles/matching/suggestions/{id}/reject")
    @Operation(
            summary = "Reject a pending suggestion",
            description = "Marks the suggestion rejected without changing its article's group assignment.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Suggestion rejected"),
                @ApiResponse(responseCode = "404", description = "Suggestion not found"),
                @ApiResponse(responseCode = "409", description = "Suggestion is not pending")
            }
    )
    public Mono<ResponseEntity<Void>> rejectSuggestion(
            @PathVariable @Parameter(description = "Id of the suggestion") Long id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        final ResponseEntity<Void> conflict = ResponseEntity.<Void>status(HttpStatus.CONFLICT).build();
        return Mono.fromRunnable(() -> articleGroupSuggestionService.reject(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound)
                .onErrorReturn(IllegalStateException.class, conflict);
    }

    /**
     * Runs the heuristic matcher over every currently ungrouped article.
     *
     * @return a Mono with 200 OK and a summary of the run
     */
    @PostMapping("/articles/matching/run")
    @Operation(
            summary = "Run the heuristic matcher",
            description = "Runs the local string-similarity heuristic over every ungrouped article, auto-assigning "
                    + "high-confidence matches and queueing medium-confidence ones for manual review.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Run completed",
                        content = @Content(schema = @Schema(implementation = MatchingRunResultDTO.class))
                )
            }
    )
    public Mono<ResponseEntity<MatchingRunResultDTO>> runHeuristicBackfill() {
        return Mono.fromCallable(articleGroupSuggestionService::runHeuristicBackfill)
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /**
     * Runs the LLM-backed matcher over every ungrouped article without a pending suggestion.
     *
     * @return a Mono with 200 OK and a summary of the run, or 502 if the LLM call fails
     */
    @PostMapping("/articles/matching/llm-run")
    @Operation(
            summary = "Run the LLM-backed matcher",
            description = "Runs the LLM-backed matcher over every ungrouped article without a pending suggestion, "
                    + "applying every confident match directly.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Run completed",
                        content = @Content(schema = @Schema(implementation = MatchingRunResultDTO.class))
                ),
                @ApiResponse(responseCode = "502", description = "The LLM-backed matching call failed")
            }
    )
    public Mono<ResponseEntity<MatchingRunResultDTO>> runLlmBatch() {
        return articleGroupSuggestionService.runLlmBatch()
                .map(ResponseEntity::ok)
                .onErrorReturn(ArticleGroupMatchingException.class, ResponseEntity.status(HttpStatus.BAD_GATEWAY).build());
    }
}
