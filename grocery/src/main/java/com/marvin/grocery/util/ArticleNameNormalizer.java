package com.marvin.grocery.util;

import java.util.Locale;

/**
 * Normalizes free-text product names into a canonical form so that names differing only by
 * casing or surrounding whitespace can be matched as the same product/article.
 */
public final class ArticleNameNormalizer {

    private ArticleNameNormalizer() {
    }

    /**
     * Normalizes the given name by trimming whitespace and lower-casing it in a locale-independent way.
     *
     * @param name the raw product name
     * @return the normalized name
     */
    public static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
