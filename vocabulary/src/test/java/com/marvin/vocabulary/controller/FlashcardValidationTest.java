package com.marvin.vocabulary.controller;

import com.generated.deepl.api.TranslateTextApi;
import com.marvin.vocabulary.dictionaryapi.DictionaryClient;
import com.marvin.vocabulary.dto.Flashcard;
import com.marvin.vocabulary.service.FlashcardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(MockitoExtension.class)
class FlashcardValidationTest {

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
    void addFlashcardWhenDeckIdIsNullShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                null,
                null,
                "test-deck",
                "anki-123",
                "front text",
                "back text",
                "description",
                false
        );

        webTestClient.post()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void addFlashcardWhenFrontIsBlankShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                null,
                10,
                "test-deck",
                "anki-123",
                "",
                "back text",
                "description",
                false
        );

        webTestClient.post()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void addFlashcardWhenBackIsBlankShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                null,
                10,
                "test-deck",
                "anki-123",
                "front text",
                "",
                "description",
                false
        );

        webTestClient.post()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void addFlashcardWhenFrontIsNullShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                null,
                10,
                "test-deck",
                "anki-123",
                null,
                "back text",
                "description",
                false
        );

        webTestClient.post()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void addFlashcardWhenBackIsNullShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                null,
                10,
                "test-deck",
                "anki-123",
                "front text",
                null,
                "description",
                false
        );

        webTestClient.post()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateFlashcardWhenDeckIdIsNullShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                1,
                null,
                "test-deck",
                "anki-123",
                "front text",
                "back text",
                "description",
                false
        );

        webTestClient.put()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateFlashcardWhenFrontIsBlankShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                1,
                10,
                "test-deck",
                "anki-123",
                "   ",
                "back text",
                "description",
                false
        );

        webTestClient.put()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateFlashcardWhenBackIsBlankShouldReturnBadRequest() {
        final Flashcard invalidFlashcard = new Flashcard(
                1,
                10,
                "test-deck",
                "anki-123",
                "front text",
                "   ",
                "description",
                false
        );

        webTestClient.put()
                .uri("/vocabulary/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidFlashcard)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
