package com.marvin.nutrition.service;

/** Thrown when an OpenFoodFacts barcode lookup fails or returns no usable nutrition data. */
public class BarcodeLookupException extends RuntimeException {

    /**
     * Constructs a new exception with the given explanatory message.
     *
     * @param message human-readable reason the barcode lookup failed
     */
    public BarcodeLookupException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given message and root cause.
     *
     * @param message human-readable reason the barcode lookup failed
     * @param cause   the underlying exception
     */
    public BarcodeLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
