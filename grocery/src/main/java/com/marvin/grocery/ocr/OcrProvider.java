package com.marvin.grocery.ocr;

import reactor.core.publisher.Mono;

/** Strategy interface for OCR providers. Implementations must be swappable without changing business logic. */
public interface OcrProvider {

    /**
     * Extracts text from the given image bytes.
     *
     * @param imageBytes the raw bytes of the image to process
     * @return a Mono emitting the extracted text
     */
    Mono<String> extractText(byte[] imageBytes);
}
