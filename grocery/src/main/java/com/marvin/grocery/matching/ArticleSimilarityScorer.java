package com.marvin.grocery.matching;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

/**
 * Computes a similarity score in {@code [0.0, 1.0]} between two normalized article names, combining
 * Jaro-Winkler and Levenshtein string metrics.
 *
 * <p>Two article names can be textually very similar while describing different products purely
 * because of a differing quantity or percentage (e.g. "vollmilch 1,5%" vs. "vollmilch 3,5%"). The
 * numeric-token guard detects this case and caps the resulting score, so such pairs never cross the
 * auto-assign threshold on string similarity alone.</p>
 */
@Component
public class ArticleSimilarityScorer {

    private static final double JARO_WINKLER_WEIGHT = 0.6;
    private static final double LEVENSHTEIN_WEIGHT = 0.4;
    private static final double NUMERIC_MISMATCH_CAP = 0.85;
    private static final Pattern NUMERIC_TOKEN_PATTERN = Pattern.compile("\\d+([.,]\\d+)?");

    private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();

    /**
     * Computes the similarity between two normalized article names.
     *
     * @param normalizedA the first normalized article name
     * @param normalizedB the second normalized article name
     * @return the combined similarity score, clamped to {@code [0.0, 1.0]}
     */
    public double similarity(String normalizedA, String normalizedB) {
        final double combined = combinedScore(normalizedA, normalizedB);
        final double clamped = Math.max(0.0, Math.min(1.0, combined));
        if (hasMismatchedNumericTokens(normalizedA, normalizedB)) {
            return Math.min(clamped, NUMERIC_MISMATCH_CAP);
        }
        return clamped;
    }

    private double combinedScore(String normalizedA, String normalizedB) {
        final double jaroWinkler = jaroWinklerSimilarity.apply(normalizedA, normalizedB);
        final double levenshteinRatio = levenshteinRatio(normalizedA, normalizedB);
        return JARO_WINKLER_WEIGHT * jaroWinkler + LEVENSHTEIN_WEIGHT * levenshteinRatio;
    }

    private double levenshteinRatio(String normalizedA, String normalizedB) {
        final int maxLength = Math.max(normalizedA.length(), normalizedB.length());
        if (maxLength == 0) {
            return 1.0;
        }
        final int distance = LevenshteinDistance.getDefaultInstance().apply(normalizedA, normalizedB);
        return 1 - (double) distance / maxLength;
    }

    private boolean hasMismatchedNumericTokens(String normalizedA, String normalizedB) {
        final Set<String> tokensA = numericTokens(normalizedA);
        final Set<String> tokensB = numericTokens(normalizedB);
        return !tokensA.isEmpty() && !tokensB.isEmpty() && !tokensA.equals(tokensB);
    }

    private Set<String> numericTokens(String value) {
        final Set<String> tokens = new HashSet<>();
        final Matcher matcher = NUMERIC_TOKEN_PATTERN.matcher(value);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}
