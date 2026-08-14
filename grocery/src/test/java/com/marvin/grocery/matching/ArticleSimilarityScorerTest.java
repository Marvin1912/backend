package com.marvin.grocery.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ArticleSimilarityScorer}, verifying score bands against real German grocery examples. */
@DisplayName("ArticleSimilarityScorer Tests")
class ArticleSimilarityScorerTest {

    private static final double AUTO_ASSIGN_THRESHOLD = 0.92;
    private static final double SUGGESTION_THRESHOLD = 0.75;
    private static final double NUMERIC_MISMATCH_CAP = 0.85;

    private final ArticleSimilarityScorer scorer = new ArticleSimilarityScorer();

    @Test
    @DisplayName("Should score identical strings as 1.0")
    void similarity_IdenticalStrings_ReturnsOne() {
        final double score = scorer.similarity("vollmilch 3,5%", "vollmilch 3,5%");

        assertEquals(1.0, score);
    }

    @Test
    @DisplayName("Should score totally different strings low")
    void similarity_TotallyDifferentStrings_ReturnsLowScore() {
        final double score = scorer.similarity("vollmilch 3,5%", "klopapier 8 rollen");

        assertTrue(score < SUGGESTION_THRESHOLD, "Expected score below the suggestion threshold, was " + score);
    }

    @Test
    @DisplayName("Should score a near-identical pair differing only by whitespace above the auto-assign threshold")
    void similarity_NearIdenticalPunctuationDifference_AboveAutoAssignThreshold() {
        final double score = scorer.similarity("vollmilch 3,5%", "vollmilch 3,5 %");

        assertTrue(score >= AUTO_ASSIGN_THRESHOLD, "Expected score at or above the auto-assign threshold, was " + score);
    }

    @Test
    @DisplayName("Should score a moderately similar pair within the suggestion band")
    void similarity_ModeratelySimilarPair_WithinSuggestionBand() {
        final double score = scorer.similarity("kartoffeln", "kartoffelsalat");

        assertTrue(score >= SUGGESTION_THRESHOLD && score < AUTO_ASSIGN_THRESHOLD,
                "Expected score within [" + SUGGESTION_THRESHOLD + ", " + AUTO_ASSIGN_THRESHOLD + "), was " + score);
    }

    @Test
    @DisplayName("Should cap the score at 0.85 when otherwise near-identical strings differ in a numeric token")
    void similarity_NumericTokenMismatch_CapsScoreAtMax() {
        final double score = scorer.similarity("vollmilch 1,5%", "vollmilch 3,5%");

        assertTrue(score <= NUMERIC_MISMATCH_CAP, "Expected score capped at " + NUMERIC_MISMATCH_CAP + ", was " + score);
        assertTrue(score > SUGGESTION_THRESHOLD, "Expected score still above the suggestion threshold, was " + score);
    }

    @Test
    @DisplayName("Should not cap the score when only one side contains a numeric token")
    void similarity_OnlyOneSideHasNumericToken_DoesNotApplyCap() {
        final double score = scorer.similarity("kaffee gemahlen", "kaffee gemahlen 500g");

        assertTrue(score > NUMERIC_MISMATCH_CAP, "Expected score above the cap since only one side has a numeric token, was " + score);
    }

    @Test
    @DisplayName("Should not cap the score when the numeric tokens on both sides are equal")
    void similarity_EqualNumericTokens_DoesNotApplyCap() {
        final double score = scorer.similarity("joghurt natur 500g", "joghurt natur 500 g");

        assertTrue(score > NUMERIC_MISMATCH_CAP, "Expected score above the cap since numeric tokens match, was " + score);
    }
}
