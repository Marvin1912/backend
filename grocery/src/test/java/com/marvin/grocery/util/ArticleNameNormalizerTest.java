package com.marvin.grocery.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ArticleNameNormalizer Tests")
class ArticleNameNormalizerTest {

    @Test
    @DisplayName("Should trim surrounding whitespace and lower-case the name")
    void normalize_TrimsAndLowerCases() {
        assertEquals("milch", ArticleNameNormalizer.normalize(" Milch "));
    }

    @Test
    @DisplayName("Should normalize using ROOT locale regardless of default locale casing rules")
    void normalize_UsesRootLocale() {
        assertEquals("istanbul", ArticleNameNormalizer.normalize("ISTANBUL"));
    }

    @Test
    @DisplayName("Should treat names differing only by case and whitespace as equal after normalization")
    void normalize_CaseAndWhitespaceVariantsMatch() {
        assertEquals(ArticleNameNormalizer.normalize("Tomaten rot"), ArticleNameNormalizer.normalize("  tomaten ROT  "));
    }
}
