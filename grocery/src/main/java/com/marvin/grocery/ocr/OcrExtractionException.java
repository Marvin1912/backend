package com.marvin.grocery.ocr;

/** Thrown when an OCR provider cannot extract usable text from a response. */
public class OcrExtractionException extends RuntimeException {

    /**
     * Creates a new OcrExtractionException with the given message.
     *
     * @param message the detail message describing the extraction failure
     */
    public OcrExtractionException(String message) {
        super(message);
    }
}
