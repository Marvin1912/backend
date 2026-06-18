package com.marvin.vocabulary.controller;

import com.marvin.vocabulary.dto.Deck;
import com.marvin.vocabulary.service.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(MockitoExtension.class)
class DeckValidationTest {

    @Mock
    private DeckService deckService;

    @InjectMocks
    private DeckController deckController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(deckController).build();
    }

    @Test
    void createDeckWhenNameIsBlankShouldReturnBadRequest() {
        final Deck invalidDeck = new Deck(null, "");

        webTestClient.post()
                .uri("/vocabulary/decks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDeck)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createDeckWhenNameIsNullShouldReturnBadRequest() {
        final Deck invalidDeck = new Deck(null, null);

        webTestClient.post()
                .uri("/vocabulary/decks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDeck)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createDeckWhenNameIsWhitespaceShouldReturnBadRequest() {
        final Deck invalidDeck = new Deck(null, "   ");

        webTestClient.post()
                .uri("/vocabulary/decks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDeck)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateDeckWhenNameIsBlankShouldReturnBadRequest() {
        final Deck invalidDeck = new Deck(1, "");

        webTestClient.put()
                .uri("/vocabulary/decks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDeck)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateDeckWhenNameIsNullShouldReturnBadRequest() {
        final Deck invalidDeck = new Deck(1, null);

        webTestClient.put()
                .uri("/vocabulary/decks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDeck)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
