package com.marvin.grocery.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.ArticleDTO;
import com.marvin.grocery.dto.ArticleGroupSuggestionDTO;
import com.marvin.grocery.dto.MatchingRunResultDTO;
import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import com.marvin.grocery.entity.ArticleGroupSuggestionEntity;
import com.marvin.grocery.entity.SuggestionSource;
import com.marvin.grocery.entity.SuggestionStatus;
import com.marvin.grocery.matching.ArticleGroupLlmMatcher.LlmMatchResponse.Match;
import com.marvin.grocery.repository.ArticleGroupRepository;
import com.marvin.grocery.repository.ArticleGroupSuggestionRepository;
import com.marvin.grocery.repository.ArticleRepository;
import com.marvin.grocery.repository.ReceiptItemRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link ArticleGroupSuggestionService} covering heuristic matching, decisions, and the LLM batch path. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleGroupSuggestionService Tests")
class ArticleGroupSuggestionServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleGroupRepository articleGroupRepository;

    @Mock
    private ArticleGroupSuggestionRepository articleGroupSuggestionRepository;

    @Mock
    private ReceiptItemRepository receiptItemRepository;

    @Mock
    private ArticleSimilarityScorer scorer;

    @Mock
    private ArticleGroupLlmMatcher llmMatcher;

    private ArticleGroupSuggestionService service;

    private ArticleGroupEntity dairyGroup;
    private ArticleEntity groupedArticle;

    @BeforeEach
    void setUp() {
        service = new ArticleGroupSuggestionService(
                articleRepository,
                articleGroupRepository,
                articleGroupSuggestionRepository,
                receiptItemRepository,
                scorer,
                llmMatcher,
                new MatchingThresholds(0.92, 0.75));

        dairyGroup = new ArticleGroupEntity();
        dairyGroup.setId(10L);
        dairyGroup.setName("Dairy");

        groupedArticle = new ArticleEntity();
        groupedArticle.setId(1L);
        groupedArticle.setName("Vollmilch");
        groupedArticle.setNormalizedName("vollmilch");
        groupedArticle.setArticleGroup(dairyGroup);
    }

    @Test
    @DisplayName("matchNewArticle should assign the group directly when the best score is at or above the auto-assign threshold")
    void matchNewArticle_ScoreAboveAutoAssignThreshold_AssignsGroupDirectly() {
        final ArticleEntity newArticle = article(2L, "vollmilch 3.5%");
        when(articleRepository.findAllWithGroup()).thenReturn(List.of(groupedArticle));
        when(scorer.similarity(anyString(), anyString())).thenReturn(0.95);

        service.matchNewArticle(newArticle);

        assertSame(dairyGroup, newArticle.getArticleGroup());
        verify(articleRepository).save(newArticle);
        verify(articleGroupSuggestionRepository, never()).save(any());
    }

    @Test
    @DisplayName("matchNewArticle should queue a pending suggestion when the best score is between the two thresholds")
    void matchNewArticle_ScoreBetweenThresholds_QueuesSuggestion() {
        final ArticleEntity newArticle = article(2L, "milchprodukt");
        when(articleRepository.findAllWithGroup()).thenReturn(List.of(groupedArticle));
        when(scorer.similarity(anyString(), anyString())).thenReturn(0.80);
        when(articleGroupSuggestionRepository.existsByArticleIdAndStatus(2L, SuggestionStatus.PENDING)).thenReturn(false);

        service.matchNewArticle(newArticle);

        assertNull(newArticle.getArticleGroup());
        verify(articleRepository, never()).save(any());
        final ArgumentCaptor<ArticleGroupSuggestionEntity> captor = ArgumentCaptor.forClass(ArticleGroupSuggestionEntity.class);
        verify(articleGroupSuggestionRepository).save(captor.capture());
        assertSame(newArticle, captor.getValue().getArticle());
        assertSame(dairyGroup, captor.getValue().getSuggestedGroup());
        assertEquals(0.80, captor.getValue().getScore());
        assertEquals(SuggestionSource.HEURISTIC, captor.getValue().getSource());
        assertEquals(SuggestionStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("matchNewArticle should not assign or suggest when the best score is below the suggestion threshold")
    void matchNewArticle_ScoreBelowSuggestionThreshold_NoOps() {
        final ArticleEntity newArticle = article(2L, "klopapier");
        when(articleRepository.findAllWithGroup()).thenReturn(List.of(groupedArticle));
        when(scorer.similarity(anyString(), anyString())).thenReturn(0.30);

        service.matchNewArticle(newArticle);

        assertNull(newArticle.getArticleGroup());
        verify(articleRepository, never()).save(any());
        verify(articleGroupSuggestionRepository, never()).save(any());
    }

    @Test
    @DisplayName("matchNewArticle should no-op without scoring when there are no grouped candidates")
    void matchNewArticle_NoGroupedCandidates_NoOps() {
        final ArticleEntity newArticle = article(2L, "klopapier");
        when(articleRepository.findAllWithGroup()).thenReturn(List.of());

        service.matchNewArticle(newArticle);

        verify(scorer, never()).similarity(anyString(), anyString());
        verify(articleRepository, never()).save(any());
        verify(articleGroupSuggestionRepository, never()).save(any());
    }

    @Test
    @DisplayName("runHeuristicBackfill should chain a same-run auto-assigned article into later matches")
    void runHeuristicBackfill_ChainsSameRunAutoAssignedArticle() {
        final ArticleEntity articleA = article(2L, "milch2");
        final ArticleEntity articleB = article(3L, "milch3");
        when(articleRepository.findAllWithGroup()).thenReturn(List.of(groupedArticle, articleA, articleB));
        when(scorer.similarity(anyString(), anyString())).thenAnswer(invocation -> {
            final String a = invocation.getArgument(0);
            final String b = invocation.getArgument(1);
            if ("milch2".equals(a) && "vollmilch".equals(b)) {
                return 0.95;
            }
            if ("milch3".equals(a) && "vollmilch".equals(b)) {
                return 0.30;
            }
            if ("milch3".equals(a) && "milch2".equals(b)) {
                return 0.95;
            }
            return 0.0;
        });

        final MatchingRunResultDTO result = service.runHeuristicBackfill();

        assertEquals(2, result.candidatesEvaluated());
        assertEquals(2, result.autoAssigned());
        assertEquals(0, result.suggested());
        assertEquals(0, result.unmatched());
        assertSame(dairyGroup, articleA.getArticleGroup());
        assertSame(dairyGroup, articleB.getArticleGroup());
        verify(articleRepository).save(articleA);
        verify(articleRepository).save(articleB);
    }

    @Test
    @DisplayName("accept should assign the suggested group and mark the suggestion accepted")
    void accept_PendingSuggestion_AssignsGroupAndAccepts() {
        final ArticleEntity article = article(2L, "milch2");
        final ArticleGroupSuggestionEntity suggestion = suggestion(5L, article, dairyGroup, 0.80, SuggestionStatus.PENDING);
        when(articleGroupSuggestionRepository.findById(5L)).thenReturn(Optional.of(suggestion));
        when(receiptItemRepository.countByArticleId(2L)).thenReturn(4L);

        final ArticleDTO result = service.accept(5L);

        assertSame(dairyGroup, article.getArticleGroup());
        assertEquals(SuggestionStatus.ACCEPTED, suggestion.getStatus());
        assertEquals(2L, result.id());
        assertEquals(10L, result.groupId());
        assertEquals("Dairy", result.groupName());
        assertEquals(4L, result.purchaseCount());
        verify(articleRepository).save(article);
        verify(articleGroupSuggestionRepository).save(suggestion);
    }

    @Test
    @DisplayName("accept should throw NoSuchElementException when the suggestion does not exist")
    void accept_MissingSuggestion_ThrowsNoSuchElementException() {
        when(articleGroupSuggestionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.accept(99L));
    }

    @Test
    @DisplayName("accept should throw IllegalStateException when the suggestion is not pending")
    void accept_NotPending_ThrowsIllegalStateException() {
        final ArticleEntity article = article(2L, "milch2");
        final ArticleGroupSuggestionEntity suggestion = suggestion(5L, article, dairyGroup, 0.80, SuggestionStatus.ACCEPTED);
        when(articleGroupSuggestionRepository.findById(5L)).thenReturn(Optional.of(suggestion));

        assertThrows(IllegalStateException.class, () -> service.accept(5L));
    }

    @Test
    @DisplayName("reject should mark the suggestion rejected without touching the article's group")
    void reject_PendingSuggestion_MarksRejectedWithoutTouchingGroup() {
        final ArticleEntity article = article(2L, "milch2");
        final ArticleGroupSuggestionEntity suggestion = suggestion(5L, article, dairyGroup, 0.80, SuggestionStatus.PENDING);
        when(articleGroupSuggestionRepository.findById(5L)).thenReturn(Optional.of(suggestion));

        service.reject(5L);

        assertEquals(SuggestionStatus.REJECTED, suggestion.getStatus());
        assertNull(article.getArticleGroup());
        verify(articleRepository, never()).save(any());
        verify(articleGroupSuggestionRepository).save(suggestion);
    }

    @Test
    @DisplayName("reject should throw NoSuchElementException when the suggestion does not exist")
    void reject_MissingSuggestion_ThrowsNoSuchElementException() {
        when(articleGroupSuggestionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.reject(99L));
    }

    @Test
    @DisplayName("reject should throw IllegalStateException when the suggestion is not pending")
    void reject_NotPending_ThrowsIllegalStateException() {
        final ArticleEntity article = article(2L, "milch2");
        final ArticleGroupSuggestionEntity suggestion = suggestion(5L, article, dairyGroup, 0.80, SuggestionStatus.REJECTED);
        when(articleGroupSuggestionRepository.findById(5L)).thenReturn(Optional.of(suggestion));

        assertThrows(IllegalStateException.class, () -> service.reject(5L));
    }

    @Test
    @DisplayName("runLlmBatch should exclude articles with an existing pending suggestion from the LLM candidate set")
    void runLlmBatch_ExcludesArticlesWithPendingSuggestion() {
        final ArticleEntity withoutPending = article(2L, "kaffee");
        final ArticleEntity withPending = article(3L, "tee");
        when(articleRepository.findAllWithGroup()).thenReturn(List.of(withoutPending, withPending));
        when(articleGroupSuggestionRepository.existsByArticleIdAndStatus(2L, SuggestionStatus.PENDING)).thenReturn(false);
        when(articleGroupSuggestionRepository.existsByArticleIdAndStatus(3L, SuggestionStatus.PENDING)).thenReturn(true);
        when(articleGroupRepository.findAll()).thenReturn(List.of(dairyGroup));
        final Match match = new Match(2L, 10L, 0.9);
        final ArgumentCaptor<List<ArticleEntity>> candidatesCaptor = ArgumentCaptor.forClass(List.class);
        when(llmMatcher.matchBatch(candidatesCaptor.capture(), anyList())).thenReturn(Mono.just(List.of(match)));

        final Mono<MatchingRunResultDTO> resultMono = service.runLlmBatch();

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertEquals(1, result.candidatesEvaluated());
                    assertEquals(1, result.autoAssigned());
                    assertEquals(0, result.suggested());
                    assertEquals(0, result.unmatched());
                })
                .verifyComplete();
        assertEquals(List.of(withoutPending), candidatesCaptor.getValue());
        assertSame(dairyGroup, withoutPending.getArticleGroup());
        verify(articleRepository).save(withoutPending);
        final ArgumentCaptor<ArticleGroupSuggestionEntity> suggestionCaptor = ArgumentCaptor.forClass(ArticleGroupSuggestionEntity.class);
        verify(articleGroupSuggestionRepository).save(suggestionCaptor.capture());
        assertEquals(SuggestionSource.LLM, suggestionCaptor.getValue().getSource());
        assertEquals(SuggestionStatus.ACCEPTED, suggestionCaptor.getValue().getStatus());
        assertEquals(0.9, suggestionCaptor.getValue().getScore());
    }

    @Test
    @DisplayName("runLlmBatch should short-circuit without calling the LLM when there are no ungrouped candidates")
    void runLlmBatch_NoCandidates_ShortCircuits() {
        when(articleRepository.findAllWithGroup()).thenReturn(List.of(groupedArticle));
        when(articleGroupRepository.findAll()).thenReturn(List.of(dairyGroup));

        final Mono<MatchingRunResultDTO> resultMono = service.runLlmBatch();

        StepVerifier.create(resultMono)
                .expectNext(new MatchingRunResultDTO(0, 0, 0, 0))
                .verifyComplete();
        verify(llmMatcher, never()).matchBatch(any(), any());
    }

    @Test
    @DisplayName("runLlmBatch should short-circuit without calling the LLM when there are no existing groups")
    void runLlmBatch_NoGroups_ShortCircuits() {
        final ArticleEntity ungrouped = article(2L, "kaffee");
        when(articleRepository.findAllWithGroup()).thenReturn(List.of(ungrouped));
        when(articleGroupSuggestionRepository.existsByArticleIdAndStatus(2L, SuggestionStatus.PENDING)).thenReturn(false);
        when(articleGroupRepository.findAll()).thenReturn(List.of());

        final Mono<MatchingRunResultDTO> resultMono = service.runLlmBatch();

        StepVerifier.create(resultMono)
                .expectNext(new MatchingRunResultDTO(0, 0, 0, 0))
                .verifyComplete();
        verify(llmMatcher, never()).matchBatch(any(), any());
    }

    @Test
    @DisplayName("listPending should map pending suggestions to DTOs")
    void listPending_ReturnsMappedDTOs() {
        final ArticleEntity article = article(2L, "milch2");
        final ArticleGroupSuggestionEntity suggestion = suggestion(5L, article, dairyGroup, 0.80, SuggestionStatus.PENDING);
        when(articleGroupSuggestionRepository.findAllWithDetailsByStatus(SuggestionStatus.PENDING)).thenReturn(List.of(suggestion));

        final List<ArticleGroupSuggestionDTO> result = service.listPending();

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals(2L, result.get(0).articleId());
        assertEquals(10L, result.get(0).suggestedGroupId());
        assertEquals(0.80, result.get(0).score());
        assertEquals(SuggestionSource.HEURISTIC, result.get(0).source());
    }

    private ArticleEntity article(Long id, String normalizedName) {
        final ArticleEntity article = new ArticleEntity();
        article.setId(id);
        article.setName(normalizedName);
        article.setNormalizedName(normalizedName);
        return article;
    }

    private ArticleGroupSuggestionEntity suggestion(
            Long id, ArticleEntity article, ArticleGroupEntity group, double score, SuggestionStatus status) {
        final ArticleGroupSuggestionEntity suggestion = new ArticleGroupSuggestionEntity();
        suggestion.setId(id);
        suggestion.setArticle(article);
        suggestion.setSuggestedGroup(group);
        suggestion.setScore(score);
        suggestion.setSource(SuggestionSource.HEURISTIC);
        suggestion.setStatus(status);
        return suggestion;
    }
}
