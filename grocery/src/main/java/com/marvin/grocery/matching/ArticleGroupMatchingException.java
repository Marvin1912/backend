package com.marvin.grocery.matching;

/** Thrown when the automated article-to-group matching process fails. */
public class ArticleGroupMatchingException extends RuntimeException {

    /**
     * Creates a new ArticleGroupMatchingException with the given message.
     *
     * @param message the detail message describing the matching failure
     */
    public ArticleGroupMatchingException(String message) {
        super(message);
    }

    /**
     * Creates a new ArticleGroupMatchingException with the given message and cause.
     *
     * @param message the detail message describing the matching failure
     * @param cause   the underlying cause
     */
    public ArticleGroupMatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
