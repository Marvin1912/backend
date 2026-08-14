package com.marvin.grocery.matching;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configurable score thresholds controlling automated article-to-group matching decisions. Bundled
 * into a single injectable value object so {@link ArticleGroupSuggestionService} can stay within a
 * 7-parameter constructor alongside its repository and matcher collaborators.
 *
 * @param autoAssignThreshold minimum {@link ArticleSimilarityScorer} score at which a match is applied automatically
 * @param suggestionThreshold minimum {@link ArticleSimilarityScorer} score at which a match is queued as a pending suggestion
 */
@Component
public record MatchingThresholds(
        @Value("${grocery.matching.auto-assign-threshold:0.92}") double autoAssignThreshold,
        @Value("${grocery.matching.suggestion-threshold:0.75}") double suggestionThreshold) {
}
