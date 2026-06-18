package com.marvin.vocabulary.controller;

import com.marvin.vocabulary.dto.Deck;
import com.marvin.vocabulary.model.DeckEntity;
import com.marvin.vocabulary.service.DeckService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** REST controller for managing vocabulary decks. */
@Validated
@RestController
@RequestMapping("/vocabulary")
public class DeckController {

    private final DeckService deckService;

    /**
     * Constructs a new DeckController with the required service.
     *
     * @param deckService the deck service
     */
    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    /**
     * Converts a DeckEntity to a Deck DTO.
     *
     * @param entity the entity to convert
     * @return the corresponding Deck DTO
     */
    private static Deck toDto(DeckEntity entity) {
        return new Deck(entity.getId(), entity.getName());
    }

    /**
     * Retrieves a specific deck by ID.
     *
     * @param id the deck ID
     * @return a Mono containing the deck or 404 if not found
     */
    @GetMapping("/decks/{id}")
    public Mono<ResponseEntity<Deck>> getDeck(@PathVariable int id) {
        DeckEntity entity = deckService.get(id);
        if (entity == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok(toDto(entity)));
    }

    /**
     * Retrieves all decks.
     *
     * @return a Flux of all decks
     */
    @GetMapping("/decks")
    public Flux<Deck> getDecks() {
        List<DeckEntity> decks = deckService.getAll();
        return Flux.fromIterable(decks).map(DeckController::toDto);
    }

    /**
     * Creates a new deck.
     *
     * @param deck the deck to create
     * @return a Mono with the created deck location response
     */
    @PostMapping("/decks")
    public Mono<ResponseEntity<Void>> createDeck(@RequestBody @Valid Deck deck) {
        final DeckEntity saved = deckService.create(deck);
        return Mono.just(ResponseEntity.created(
            URI.create("/decks/" + saved.getId())
        ).build());
    }

    /**
     * Updates an existing deck.
     *
     * @param deck the deck to update
     * @return a Mono with the updated deck
     */
    @PutMapping("/decks")
    public Mono<ResponseEntity<Deck>> updateDeck(@RequestBody @Valid Deck deck) {
        final DeckEntity deckEntity = deckService.update(deck);
        return Mono.just(ResponseEntity.ok(toDto(deckEntity)));
    }
}
