package com.marvin.vocabulary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.vocabulary.dto.Flashcard;
import com.marvin.vocabulary.model.DeckEntity;
import com.marvin.vocabulary.model.FlashcardEntity;
import com.marvin.vocabulary.repository.DeckRepository;
import com.marvin.vocabulary.repository.FlashcardRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private DeckRepository deckRepository;

    @InjectMocks
    private FlashcardService flashcardService;

    private DeckEntity deck;
    private DeckEntity reverseDeck;

    @BeforeEach
    void setUp() {
        deck = new DeckEntity(1, "deck", null);
        reverseDeck = new DeckEntity(2, "deck_reversed", deck);
        deck.setReverseDeck(reverseDeck);
    }

    @Test
    void saveShouldPersistBothFlashcardsExactlyOnceAndWireReverseReferences() {
        final Flashcard flashcard = new Flashcard(
                null,
                deck.getId(),
                deck.getName(),
                "anki-1",
                "front",
                "back",
                "description",
                false
        );

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));

        final FlashcardEntity savedOriginal = new FlashcardEntity(
                10, deck, null, "anki-1", "front", "back", "description", false);
        final FlashcardEntity savedReverse = new FlashcardEntity(
                11, reverseDeck, null, null, "back", "front", "description", false);

        when(flashcardRepository.save(any(FlashcardEntity.class)))
                .thenReturn(savedOriginal)
                .thenReturn(savedReverse);

        final FlashcardEntity result = flashcardService.save(flashcard);

        verify(flashcardRepository, times(2)).save(any(FlashcardEntity.class));
        assertThat(result).isEqualTo(savedOriginal);
        assertThat(result.getReverseFlashcard()).isEqualTo(savedReverse);
        assertThat(savedReverse.getReverseFlashcard()).isEqualTo(savedOriginal);
    }

}
