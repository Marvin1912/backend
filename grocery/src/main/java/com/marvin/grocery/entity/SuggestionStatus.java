package com.marvin.grocery.entity;

/** Lifecycle state of an {@link ArticleGroupSuggestionEntity}. */
public enum SuggestionStatus {

    /** Awaiting a manual accept/reject decision. */
    PENDING,

    /** The suggested group assignment was applied. */
    ACCEPTED,

    /** The suggested group assignment was discarded. */
    REJECTED
}
