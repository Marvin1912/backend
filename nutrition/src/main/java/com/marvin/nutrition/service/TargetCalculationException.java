package com.marvin.nutrition.service;

/** Thrown when target calories cannot be computed due to missing or invalid profile/weight data. */
public class TargetCalculationException extends RuntimeException {

    /**
     * Constructs a new exception with the given explanatory message.
     *
     * @param message human-readable reason the calculation failed
     */
    public TargetCalculationException(String message) {
        super(message);
    }
}
