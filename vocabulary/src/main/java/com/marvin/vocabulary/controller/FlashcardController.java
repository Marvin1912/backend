package com.marvin.vocabulary.controller;

import com.generated.deepl.api.TranslateTextApi;
import com.generated.deepl.model.SourceLanguageText;
import com.generated.deepl.model.TargetLanguageText;
import com.generated.deepl.model.TranslateText200ResponseTranslationsInner;
import com.generated.deepl.model.TranslateTextRequest;
import com.marvin.vocabulary.dictionaryapi.DictionaryClient;
import com.marvin.vocabulary.dto.DictionaryEntry;
import com.marvin.vocabulary.dto.Flashcard;
import com.marvin.vocabulary.dto.Translation;
import com.marvin.vocabulary.exceptions.DictionaryApiException;
import com.marvin.vocabulary.exceptions.DictionaryServiceUnavailableException;
import com.marvin.vocabulary.exceptions.InvalidWordException;
import com.marvin.vocabulary.exceptions.RateLimitExceededException;
import com.marvin.vocabulary.exceptions.WordNotFoundException;
import com.marvin.vocabulary.model.FlashcardEntity;
import com.marvin.vocabulary.service.FlashcardService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST controller for managing vocabulary flashcards and translations. Provides endpoints for CRUD operations on flashcards, file operations, and translation
 * services via DeepL API.
 */
@Slf4j
@RestController
@RequestMapping("/vocabulary")
public class FlashcardController {

    private static final String CSV_FILENAME = "Standard.csv";
    private static final String CSV_MEDIA_TYPE = "text/csv";

    private final DictionaryClient dictionaryClient;
    private final FlashcardService flashcardService;
    private final TranslateTextApi translateTextApi;

    /**
     * Constructs a new FlashcardController with required dependencies.
     *
     * @param dictionaryClient the dictionary service client
     * @param flashcardService the flashcard service
     * @param translateTextApi the translation API client
     */
    public FlashcardController(
            DictionaryClient dictionaryClient,
            FlashcardService flashcardService,
            TranslateTextApi translateTextApi
    ) {
        this.dictionaryClient = dictionaryClient;
        this.flashcardService = flashcardService;
        this.translateTextApi = translateTextApi;
    }

    /**
     * Converts a blocking operation to a reactive Mono running on bounded elastic scheduler.
     *
     * @param callable the blocking operation to execute
     * @param <T>      the return type
     * @return a Mono wrapping the callable result
     */
    private static <T> Mono<T> executeBlocking(Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates a standardized error response for exceptions.
     *
     * @param throwable the exception that occurred
     * @param message   the error message
     * @return a ResponseEntity with error details
     */
    private static ResponseEntity<Map<String, String>> createErrorResponse(Throwable throwable,
            String message) {
        log.error("Request processing failed", throwable);
        return ResponseEntity.internalServerError().body(
                Map.of(
                        "type", throwable.getClass().getSimpleName(),
                        "message", message
                )
        );
    }

    /**
     * Converts a FlashcardEntity to a Flashcard DTO.
     *
     * @param entity the entity to convert
     * @return the corresponding Flashcard DTO
     */
    private static Flashcard convertToDto(FlashcardEntity entity) {
        return new Flashcard(
                entity.getId(),
                entity.getDeck().getId(),
                entity.getDeck().getName(),
                entity.getAnkiId(),
                entity.getFront(),
                entity.getBack(),
                entity.getDescription(),
                entity.isUpdated()
        );
    }

    /**
     * Converts file part content to byte array.
     *
     * @param filePart the file part to convert
     * @return a Mono containing the file content as byte array
     */
    private static Mono<byte[]> convertFilePartToBytes(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return Mono.just(bytes);
                });
    }

    /**
     * Creates a TranslateTextRequest for the given parameters.
     *
     * @param word           the word to translate
     * @param context        the translation context
     * @param sourceLanguage the source language
     * @param targetLanguage the target language
     * @return a configured TranslateTextRequest
     */
    private TranslateTextRequest createTranslationRequest(String word, String context,
            SourceLanguageText sourceLanguage,
            TargetLanguageText targetLanguage) {
        TranslateTextRequest request = new TranslateTextRequest();
        request.setText(List.of(word));
        request.setContext(context);
        request.setSourceLang(sourceLanguage);
        request.setTargetLang(targetLanguage);
        return request;
    }

    /**
     * Creates a CSV file download response.
     *
     * @param fileContent the file content as byte array
     * @return a ResponseEntity configured for file download
     */
    private ResponseEntity<byte[]> createFileDownloadResponse(byte[] fileContent) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("filename", CSV_FILENAME)
                .contentLength(fileContent.length)
                .body(fileContent);
    }

    /**
     * Handles general exceptions in the controller.
     *
     * @param exception the exception that occurred
     * @return an error response
     */
    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleException(Exception exception) {
        return createErrorResponse(exception, exception.getMessage());
    }

    /**
     * Handles WebClient response exceptions.
     *
     * @param exception the WebClient response exception
     * @return an error response
     */
    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleWebClientResponseException(
            WebClientResponseException exception) {
        return createErrorResponse(exception, exception.getResponseBodyAsString());
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleWordNotFoundException(
            WordNotFoundException ex) {
        log.warn("Word not found: {}", ex.getWord());
        return ResponseEntity.status(404).body(
                Map.of(
                        "type", "WORD_NOT_FOUND",
                        "message", ex.getMessage(),
                        "word", ex.getWord()
                )
        );
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleInvalidWordException(InvalidWordException ex) {
        log.warn("Invalid word provided: {}", ex.getWord());
        return ResponseEntity.badRequest().body(
                Map.of(
                        "type", "INVALID_WORD",
                        "message", ex.getMessage(),
                        "word", ex.getWord()
                )
        );
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleRateLimitExceededException(
            RateLimitExceededException ex) {
        log.warn("Rate limit exceeded");
        return ResponseEntity.status(429).body(
                Map.of(
                        "type", "RATE_LIMIT_EXCEEDED",
                        "message", ex.getMessage()
                )
        );
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleDictionaryServiceUnavailableException(
            DictionaryServiceUnavailableException ex) {
        log.error("Dictionary service unavailable");
        return ResponseEntity.status(503).body(
                Map.of(
                        "type", "SERVICE_UNAVAILABLE",
                        "message", ex.getMessage()
                )
        );
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleDictionaryApiException(
            DictionaryApiException ex) {
        log.error("Dictionary API error: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(
                Map.of(
                        "type", ex.getErrorType(),
                        "message", ex.getMessage(),
                        "statusCode", String.valueOf(ex.getStatusCode())
                )
        );
    }

    /**
     * Retrieves dictionary information for a specific word.
     *
     * @param word the word to look up
     * @return a Mono containing dictionary entries for the word
     */
    @GetMapping("/words/{word}")
    public Mono<List<DictionaryEntry>> getWord(@PathVariable String word) {
        return dictionaryClient.getWord(word);
    }

    /**
     * Retrieves a specific flashcard by ID.
     *
     * @param id the flashcard ID
     * @return a Mono containing the flashcard or 404 if not found
     */
    @GetMapping("/flashcards/{id}")
    public Mono<ResponseEntity<Flashcard>> getFlashcard(@PathVariable int id) {
        FlashcardEntity flashcardEntity = flashcardService.get(id);

        if (flashcardEntity == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        return Mono.just(convertToDto(flashcardEntity))
                .map(ResponseEntity::ok);
    }

    /**
     * Retrieves flashcards with optional filtering criteria.
     *
     * @param missing optional filter for missing flashcards
     * @param updated optional filter for updated status
     * @return a Flux of matching flashcards
     */
    @GetMapping("/flashcards")
    public Flux<Flashcard> getFlashcards(
            @RequestParam(required = false) String missing,
            @RequestParam(required = false) Boolean updated
    ) {
        return Flux.fromIterable(flashcardService.get(missing, updated))
                .map(FlashcardController::convertToDto);
    }

    /**
     * Downloads all flashcards as a CSV file.
     *
     * @return a Mono containing the CSV file download response
     */
    @GetMapping("/flashcards/file")
    public Mono<ResponseEntity<byte[]>> getFile() {
        try {
            byte[] fileContent = flashcardService.getFile();
            ResponseEntity<byte[]> responseEntity = createFileDownloadResponse(fileContent);
            return Mono.just(responseEntity);
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }

    /**
     * Creates a new flashcard.
     *
     * @param flashcardMono a Mono containing the flashcard to create
     * @return a Mono with the created flashcard location response
     */
    @PostMapping("/flashcards")
    public Mono<ResponseEntity<Void>> addFlashcard(@RequestBody Mono<Flashcard> flashcardMono) {
        return flashcardMono
                .flatMap(entity -> executeBlocking(() -> flashcardService.save(entity)))
                .map(savedEntity -> ResponseEntity.created(
                        URI.create("/flashcards/" + savedEntity.getId())
                ).build());
    }

    /**
     * Updates an existing flashcard.
     *
     * @param flashcardMono a Mono containing the flashcard to update
     * @return a Mono with no content response
     */
    @PutMapping("/flashcards")
    public Mono<ResponseEntity<Void>> updateFlashcard(@RequestBody Mono<Flashcard> flashcardMono) {
        return flashcardMono
                .flatMap(entity -> executeBlocking(() -> flashcardService.update(entity)))
                .map(updateResult -> ResponseEntity.noContent().build());
    }

    /**
     * Imports flashcards from a CSV file.
     *
     * @param fileMono a Mono containing the file part to import
     * @return a Mono with no content response
     */
    @PutMapping("/flashcards/file")
    public Mono<ResponseEntity<Void>> updateFlashcards(
            @RequestPart("file") Mono<FilePart> fileMono) {
        return fileMono
                .flatMap(FlashcardController::convertFilePartToBytes)
                .switchIfEmpty(Mono.just(new byte[0]))
                .flatMap(fileBytes -> executeBlocking(
                        () -> flashcardService.importFlashcards(fileBytes)))
                .onErrorResume(exception -> {
                    log.error("Failed to import flashcards", exception);
                    return Mono.just(0);
                })
                .map(importResult -> ResponseEntity.noContent().build());
    }

    /**
     * Translates a word using DeepL API.
     *
     * @param word           the word to translate
     * @param context        the translation context
     * @param sourceLanguage the source language (default: EN)
     * @param targetLanguage the target language (default: DE)
     * @return a Flux of translation results
     */
    @GetMapping("/flashcards/translations")
    public Flux<Translation> getTranslation(
            @RequestParam String word,
            @RequestParam String context,
            @RequestParam(defaultValue = "EN") SourceLanguageText sourceLanguage,
            @RequestParam(defaultValue = "DE") TargetLanguageText targetLanguage
    ) {
        TranslateTextRequest translationRequest = createTranslationRequest(
                word, context, sourceLanguage, targetLanguage
        );

        return translateTextApi.translateText(translationRequest)
                .flatMapMany(translationResponse -> {
                    List<TranslateText200ResponseTranslationsInner> deepLTranslations = translationResponse.getTranslations();

                    if (deepLTranslations == null || deepLTranslations.isEmpty()) {
                        return Flux.empty();
                    }

                    return Flux.fromIterable(deepLTranslations)
                            .map(deepLTranslation -> new Translation(deepLTranslation.getText()));
                });
    }
}
