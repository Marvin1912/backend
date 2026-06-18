package com.marvin.vocabulary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.generated.deepl.ApiClient;
import com.generated.deepl.api.TranslateTextApi;
import com.generated.deepl.model.TranslateText200Response;
import com.generated.deepl.model.TranslateText200ResponseTranslationsInner;
import com.marvin.vocabulary.dictionaryapi.DictionaryClient;
import com.marvin.vocabulary.dto.DictionaryEntry;
import com.marvin.vocabulary.dto.Flashcard;
import com.marvin.vocabulary.dto.Translation;
import com.marvin.vocabulary.model.DeckEntity;
import com.marvin.vocabulary.model.FlashcardEntity;
import com.marvin.vocabulary.service.FlashcardService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class FlashcardControllerTest {

    private final DeckEntity testDeck = new DeckEntity(10, "test-deck", null);
    private final FlashcardEntity testFlashcardEntity = new FlashcardEntity(
            1,
            testDeck,
            null,
            "anki-123",
            "test front",
            "test back",
            "test description",
            true
    );
    private final Flashcard testFlashcard = new Flashcard(
            1,
            10,
            "test-deck",
            "anki-123",
            "test front",
            "test back",
            "test description",
            true
    );
    @Mock
    private DictionaryClient dictionaryClient;
    @Mock
    private FlashcardService flashcardService;
    @Mock
    private TranslateTextApi translateTextApi;
    @InjectMocks
    private FlashcardController flashcardController;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(flashcardController).build();
    }

    @Test
    void getWordShouldReturnDictionaryEntries() {
        final List<DictionaryEntry> expectedEntries = List.of(new DictionaryEntry(
                "hello",
                "/həˈloʊ/",
                List.of(),
                List.of(),
                null,
                List.of("https://example.com/hello")
        ));

        when(dictionaryClient.getWord("hello")).thenReturn(Mono.just(expectedEntries));

        webTestClient.get()
                .uri("/vocabulary/words/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DictionaryEntry.class)
                .isEqualTo(expectedEntries);
    }

    @Test
    void getTranslationShouldReturnTranslationsWithoutMutatingSharedApiClient() {
        final TranslateText200ResponseTranslationsInner translationItem = new TranslateText200ResponseTranslationsInner();
        translationItem.setText("Hallo");
        final TranslateText200Response response = new TranslateText200Response();
        response.setTranslations(List.of(translationItem));

        when(translateTextApi.translateText(any())).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/vocabulary/flashcards/translations?word=hello&context=greeting&sourceLanguage=EN&targetLanguage=DE")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Translation.class)
                .isEqualTo(List.of(new Translation("Hallo")));

        Mockito.verify(translateTextApi, Mockito.never()).getApiClient();
        Mockito.verify(translateTextApi, Mockito.never()).setApiClient(Mockito.any(ApiClient.class));
    }

    @Test
    void getFlashcardWhenFlashcardExistsShouldReturnFlashcard() {
        when(flashcardService.get(1)).thenReturn(testFlashcardEntity);

        webTestClient.get()
                .uri("/vocabulary/flashcards/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Flashcard.class)
                .isEqualTo(testFlashcard);
    }

    @Test
    void getFlashcardWhenFlashcardNotExistsShouldReturnNotFound() {
        when(flashcardService.get(999)).thenReturn(null);

        webTestClient.get()
                .uri("/vocabulary/flashcards/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getFlashcardsWithoutParamsShouldReturnAllFlashcards() {
        final List<FlashcardEntity> flashcardEntities = List.of(testFlashcardEntity);
        final List<Flashcard> expectedFlashcards = List.of(testFlashcard);

        when(flashcardService.get(null, null)).thenReturn(flashcardEntities);

        webTestClient.get()
                .uri("/vocabulary/flashcards")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Flashcard.class)
                .isEqualTo(expectedFlashcards);
    }

    @Test
    void getFlashcardsWithMissingAnkiIdParamShouldReturnFlashcardsWithoutAnkiId() {
        final List<FlashcardEntity> flashcardEntities = List.of(testFlashcardEntity);
        final List<Flashcard> expectedFlashcards = List.of(testFlashcard);

        when(flashcardService.get("ankiId", null)).thenReturn(flashcardEntities);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/vocabulary/flashcards")
                        .queryParam("missing", "ankiId")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Flashcard.class)
                .isEqualTo(expectedFlashcards);
    }

    @Test
    void getFlashcardsWithUpdatedParamShouldReturnUpdatedFlashcards() {
        final List<FlashcardEntity> flashcardEntities = List.of(testFlashcardEntity);
        final List<Flashcard> expectedFlashcards = List.of(testFlashcard);

        when(flashcardService.get(null, true)).thenReturn(flashcardEntities);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/vocabulary/flashcards")
                        .queryParam("updated", "true")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Flashcard.class)
                .isEqualTo(expectedFlashcards);
    }

    @Test
    void getFileShouldReturnStreamingCsvFile() {
        final byte[] csvBytes = "#separator:tab\n#html:false\n#guid column:1\ntest-content"
                .getBytes(StandardCharsets.UTF_8);
        final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(csvBytes);

        when(flashcardService.streamCsvFile()).thenReturn(Flux.just(dataBuffer));

        webTestClient.get()
                .uri("/vocabulary/flashcards/file")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv")
                .expectHeader().valueEquals("Content-Disposition", "attachment")
                .expectHeader().valueEquals("filename", "Standard.csv")
                .expectBody(byte[].class)
                .isEqualTo(csvBytes);
    }

    @Test
    void getFileWhenStreamEmitsShouldReturnInternalServerError() {
        when(flashcardService.streamCsvFile())
                .thenReturn(Flux.error(new RuntimeException("File generation failed")));

        webTestClient.get()
                .uri("/vocabulary/flashcards/file")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void addFlashcardShouldCreateFlashcardAndReturnLocation() {
        final Flashcard newFlashcard = new Flashcard(
                null,
                10,
                "test-deck",
                "anki-456",
                "new front",
                "new back",
                "new description",
                false
        );

        final FlashcardEntity savedEntity = new FlashcardEntity(
                2,
                testDeck,
                null,
                "anki-456",
                "new front",
                "new back",
                "new description",
                false
        );

        when(flashcardService.save(any(Flashcard.class))).thenReturn(savedEntity);

        webTestClient.post()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newFlashcard)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/flashcards/2")
                .expectBody().isEmpty();
    }

    @Test
    void updateFlashcardShouldUpdateFlashcardAndReturnNoContent() {
        when(flashcardService.update(any(Flashcard.class))).thenReturn(1);

        webTestClient.put()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testFlashcard)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void handleExceptionShouldReturnErrorResponse() {
        // Force an exception by mocking the service to throw an exception
        when(dictionaryClient.getWord(any())).thenThrow(new RuntimeException("Test exception"));

        webTestClient.get()
                .uri("/vocabulary/words/test")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Map.class)
                .value(response -> {
                    assert response.containsKey("type");
                    assert response.containsKey("message");
                });
    }

}
