package com.marvin.grocery.entity;

/** Identifies which matching mechanism produced an {@link ArticleGroupSuggestionEntity}. */
public enum SuggestionSource {

    /** The suggestion was produced by the local string-similarity heuristic. */
    HEURISTIC,

    /** The suggestion was produced by the LLM-backed matcher. */
    LLM
}
