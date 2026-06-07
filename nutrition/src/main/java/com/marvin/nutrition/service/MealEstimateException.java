package com.marvin.nutrition.service;

/** Thrown when a canteen meal description cannot be estimated into a valid {@link com.marvin.nutrition.dto.MealEstimateDTO}. */
public class MealEstimateException extends RuntimeException {

    /**
     * Constructs a new exception with the given explanatory message.
     *
     * @param message human-readable reason the meal could not be estimated
     */
    public MealEstimateException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given message and root cause.
     *
     * @param message human-readable reason the meal could not be estimated
     * @param cause   the underlying exception
     */
    public MealEstimateException(String message, Throwable cause) {
        super(message, cause);
    }
}
