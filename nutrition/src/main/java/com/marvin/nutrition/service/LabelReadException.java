package com.marvin.nutrition.service;

/** Thrown when a nutrition label image cannot be parsed into a valid {@link com.marvin.nutrition.dto.FoodDraftDTO}. */
public class LabelReadException extends RuntimeException {

    /**
     * Constructs a new exception with the given explanatory message.
     *
     * @param message human-readable reason the label could not be read
     */
    public LabelReadException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given message and root cause.
     *
     * @param message human-readable reason the label could not be read
     * @param cause   the underlying exception
     */
    public LabelReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
