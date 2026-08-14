package com.marvin.grocery.matching;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Matches ungrouped {@link ArticleEntity} rows to existing {@link ArticleGroupEntity} rows, either
 * automatically (score above the auto-assign threshold), by queueing a pending
 * {@link ArticleGroupSuggestionEntity} for manual review (score above the suggestion threshold), or
 * by asking the LLM-backed matcher to fill in the rest in a single batched call.
 */
@Service
public class ArticleGroupSuggestionService {

    private final ArticleRepository articleRepository;
    private final ArticleGroupRepository articleGroupRepository;
    private final ArticleGroupSuggestionRepository articleGroupSuggestionRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final ArticleSimilarityScorer scorer;
    private final ArticleGroupLlmMatcher llmMatcher;
    private final MatchingThresholds thresholds;

    /**
     * Creates a new ArticleGroupSuggestionService with all required dependencies.
     *
     * @param articleRepository               the JPA repository for articles
     * @param articleGroupRepository          the JPA repository for article groups
     * @param articleGroupSuggestionRepository the JPA repository for article group suggestions
     * @param receiptItemRepository           the JPA repository for receipt items, used for purchase counts
     * @param scorer                          the local string-similarity scorer used for the heuristic pass
     * @param llmMatcher                      the LLM-backed matcher used for the batch fallback pass
     * @param thresholds                      the configured auto-assign and suggestion score thresholds
     */
    public ArticleGroupSuggestionService(
            ArticleRepository articleRepository,
            ArticleGroupRepository articleGroupRepository,
            ArticleGroupSuggestionRepository articleGroupSuggestionRepository,
            ReceiptItemRepository receiptItemRepository,
            ArticleSimilarityScorer scorer,
            ArticleGroupLlmMatcher llmMatcher,
            MatchingThresholds thresholds) {
        this.articleRepository = articleRepository;
        this.articleGroupRepository = articleGroupRepository;
        this.articleGroupSuggestionRepository = articleGroupSuggestionRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.scorer = scorer;
        this.llmMatcher = llmMatcher;
        this.thresholds = thresholds;
    }

    /**
     * Attempts to match a newly created article against every already-grouped article, either
     * assigning it automatically, queueing a pending suggestion, or leaving it unmatched.
     *
     * @param newArticle the just-created article to attempt to match
     */
    @Transactional
    public void matchNewArticle(ArticleEntity newArticle) {
        final List<ArticleEntity> groupedCandidates = articleRepository.findAllWithGroup().stream()
                .filter(a -> a.getArticleGroup() != null)
                .toList();
        applyBestMatch(newArticle, groupedCandidates);
    }

    /**
     * Runs the heuristic matcher over every currently ungrouped article, chaining any article that
     * gets auto-assigned within this run into the candidate pool for subsequent articles.
     *
     * @return a summary of how many articles were auto-assigned, suggested, or left unmatched
     */
    @Transactional
    public MatchingRunResultDTO runHeuristicBackfill() {
        final Map<Boolean, List<ArticleEntity>> partitioned = articleRepository.findAllWithGroup().stream()
                .collect(Collectors.partitioningBy(a -> a.getArticleGroup() != null));
        final List<ArticleEntity> groupedCandidates = new ArrayList<>(partitioned.get(true));
        final List<ArticleEntity> ungrouped = partitioned.get(false);

        int autoAssigned = 0;
        int suggested = 0;
        for (final ArticleEntity article : ungrouped) {
            final MatchOutcome outcome = applyBestMatch(article, groupedCandidates);
            if (outcome == MatchOutcome.AUTO_ASSIGNED) {
                autoAssigned++;
                groupedCandidates.add(article);
            } else if (outcome == MatchOutcome.SUGGESTED) {
                suggested++;
            }
        }
        return new MatchingRunResultDTO(ungrouped.size(), autoAssigned, suggested, ungrouped.size() - autoAssigned - suggested);
    }

    /**
     * Returns every pending suggestion, ordered by score descending.
     *
     * @return list of pending suggestion DTOs
     */
    public List<ArticleGroupSuggestionDTO> listPending() {
        return articleGroupSuggestionRepository.findAllWithDetailsByStatus(SuggestionStatus.PENDING).stream()
                .map(this::toSuggestionDTO)
                .toList();
    }

    /**
     * Accepts a pending suggestion: assigns its article to the suggested group and marks the
     * suggestion as accepted.
     *
     * @param suggestionId the id of the suggestion to accept
     * @return the updated article as a DTO
     * @throws NoSuchElementException if no suggestion with that id exists
     * @throws IllegalStateException  if the suggestion is not currently pending
     */
    @Transactional
    public ArticleDTO accept(Long suggestionId) {
        final ArticleGroupSuggestionEntity suggestion = findSuggestionOrThrow(suggestionId);
        requirePending(suggestion, suggestionId);
        final ArticleEntity article = suggestion.getArticle();
        final ArticleGroupEntity group = suggestion.getSuggestedGroup();
        article.setArticleGroup(group);
        articleRepository.save(article);
        suggestion.setStatus(SuggestionStatus.ACCEPTED);
        articleGroupSuggestionRepository.save(suggestion);
        return toArticleDTO(article, group);
    }

    /**
     * Rejects a pending suggestion without touching its article's group assignment.
     *
     * @param suggestionId the id of the suggestion to reject
     * @throws NoSuchElementException if no suggestion with that id exists
     * @throws IllegalStateException  if the suggestion is not currently pending
     */
    @Transactional
    public void reject(Long suggestionId) {
        final ArticleGroupSuggestionEntity suggestion = findSuggestionOrThrow(suggestionId);
        requirePending(suggestion, suggestionId);
        suggestion.setStatus(SuggestionStatus.REJECTED);
        articleGroupSuggestionRepository.save(suggestion);
    }

    /**
     * Runs the LLM-backed matcher over every ungrouped article that does not already have a pending
     * suggestion, applying every returned match directly (no manual accept/reject step) and
     * recording an already-accepted suggestion row per match for traceability.
     *
     * @return a Mono emitting a summary of how many articles were evaluated and matched
     */
    public Mono<MatchingRunResultDTO> runLlmBatch() {
        return Mono.fromCallable(this::loadLlmCandidates)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::runLlmBatchForCandidates);
    }

    private LlmCandidates loadLlmCandidates() {
        final List<ArticleEntity> ungrouped = articleRepository.findAllWithGroup().stream()
                .filter(a -> a.getArticleGroup() == null)
                .filter(a -> !articleGroupSuggestionRepository.existsByArticleIdAndStatus(a.getId(), SuggestionStatus.PENDING))
                .toList();
        final List<ArticleGroupEntity> groups = articleGroupRepository.findAll();
        return new LlmCandidates(ungrouped, groups);
    }

    private Mono<MatchingRunResultDTO> runLlmBatchForCandidates(LlmCandidates candidates) {
        if (candidates.articles().isEmpty() || candidates.groups().isEmpty()) {
            return Mono.just(new MatchingRunResultDTO(0, 0, 0, 0));
        }
        return llmMatcher.matchBatch(candidates.articles(), candidates.groups())
                .flatMap(matches -> applyLlmMatches(candidates, matches));
    }

    private Mono<MatchingRunResultDTO> applyLlmMatches(LlmCandidates candidates, List<Match> matches) {
        return Mono.fromCallable(() -> {
            final Map<Long, ArticleEntity> articlesById = candidates.articles().stream()
                    .collect(Collectors.toMap(ArticleEntity::getId, Function.identity()));
            final Map<Long, ArticleGroupEntity> groupsById = candidates.groups().stream()
                    .collect(Collectors.toMap(ArticleGroupEntity::getId, Function.identity()));
            final int applied = applyMatches(matches, articlesById, groupsById);
            return new MatchingRunResultDTO(candidates.articles().size(), applied, 0, candidates.articles().size() - applied);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private int applyMatches(List<Match> matches, Map<Long, ArticleEntity> articlesById, Map<Long, ArticleGroupEntity> groupsById) {
        int applied = 0;
        for (final Match match : matches) {
            final ArticleEntity article = articlesById.get(match.articleId());
            final ArticleGroupEntity group = groupsById.get(match.groupId());
            if (article == null || group == null) {
                continue;
            }
            applyLlmMatch(article, group, match.confidence());
            applied++;
        }
        return applied;
    }

    private void applyLlmMatch(ArticleEntity article, ArticleGroupEntity group, Double confidence) {
        article.setArticleGroup(group);
        articleRepository.save(article);
        final ArticleGroupSuggestionEntity suggestion = new ArticleGroupSuggestionEntity();
        suggestion.setArticle(article);
        suggestion.setSuggestedGroup(group);
        suggestion.setScore(confidence == null ? 0.0 : confidence);
        suggestion.setSource(SuggestionSource.LLM);
        suggestion.setStatus(SuggestionStatus.ACCEPTED);
        articleGroupSuggestionRepository.save(suggestion);
    }

    private MatchOutcome applyBestMatch(ArticleEntity article, List<ArticleEntity> groupedCandidates) {
        if (groupedCandidates.isEmpty()) {
            return MatchOutcome.NONE;
        }
        final BestMatch bestMatch = findBestMatch(article, groupedCandidates);
        if (bestMatch.score() >= thresholds.autoAssignThreshold()) {
            article.setArticleGroup(bestMatch.group());
            articleRepository.save(article);
            return MatchOutcome.AUTO_ASSIGNED;
        }
        if (bestMatch.score() >= thresholds.suggestionThreshold()) {
            return queueSuggestion(article, bestMatch.group(), bestMatch.score());
        }
        return MatchOutcome.NONE;
    }

    private BestMatch findBestMatch(ArticleEntity article, List<ArticleEntity> groupedCandidates) {
        double bestScore = -1.0;
        ArticleGroupEntity bestGroup = null;
        for (final ArticleEntity candidate : groupedCandidates) {
            final double score = scorer.similarity(article.getNormalizedName(), candidate.getNormalizedName());
            if (score > bestScore) {
                bestScore = score;
                bestGroup = candidate.getArticleGroup();
            }
        }
        return new BestMatch(bestGroup, bestScore);
    }

    private MatchOutcome queueSuggestion(ArticleEntity article, ArticleGroupEntity group, double score) {
        if (articleGroupSuggestionRepository.existsByArticleIdAndStatus(article.getId(), SuggestionStatus.PENDING)) {
            return MatchOutcome.NONE;
        }
        final ArticleGroupSuggestionEntity suggestion = new ArticleGroupSuggestionEntity();
        suggestion.setArticle(article);
        suggestion.setSuggestedGroup(group);
        suggestion.setScore(score);
        suggestion.setSource(SuggestionSource.HEURISTIC);
        suggestion.setStatus(SuggestionStatus.PENDING);
        articleGroupSuggestionRepository.save(suggestion);
        return MatchOutcome.SUGGESTED;
    }

    private ArticleGroupSuggestionEntity findSuggestionOrThrow(Long suggestionId) {
        return articleGroupSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new NoSuchElementException("Suggestion not found: " + suggestionId));
    }

    private void requirePending(ArticleGroupSuggestionEntity suggestion, Long suggestionId) {
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is not pending: " + suggestionId);
        }
    }

    private ArticleGroupSuggestionDTO toSuggestionDTO(ArticleGroupSuggestionEntity suggestion) {
        final ArticleEntity article = suggestion.getArticle();
        final ArticleGroupEntity group = suggestion.getSuggestedGroup();
        return new ArticleGroupSuggestionDTO(
                suggestion.getId(),
                article.getId(),
                article.getName(),
                article.getNormalizedName(),
                group.getId(),
                group.getName(),
                suggestion.getScore(),
                suggestion.getSource());
    }

    private ArticleDTO toArticleDTO(ArticleEntity article, ArticleGroupEntity group) {
        final long purchaseCount = receiptItemRepository.countByArticleId(article.getId());
        return new ArticleDTO(
                article.getId(),
                article.getNormalizedName(),
                article.getName(),
                group == null ? null : group.getId(),
                group == null ? null : group.getName(),
                purchaseCount);
    }

    /** Outcome of attempting to match a single article against the current grouped candidate pool. */
    private enum MatchOutcome {
        AUTO_ASSIGNED,
        SUGGESTED,
        NONE
    }

    private record BestMatch(ArticleGroupEntity group, double score) {
    }

    private record LlmCandidates(List<ArticleEntity> articles, List<ArticleGroupEntity> groups) {
    }
}
