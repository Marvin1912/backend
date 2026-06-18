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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;

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

    @Test
    void streamCsvFileShouldEmitHeaderBuffersThenOneBufferPerFlashcard() {
        final FlashcardEntity entity = new FlashcardEntity(
                1, deck, null, "anki-1", "front", "back", "description", false);

        when(flashcardRepository.findAll()).thenReturn(List.of(entity));

        StepVerifier.create(flashcardService.streamCsvFile())
                .assertNext(buf -> assertBufferContains(buf, "#separator:tab"))
                .assertNext(buf -> assertBufferContains(buf, "#html:false"))
                .assertNext(buf -> assertBufferContains(buf, "#guid column:1"))
                .assertNext(buf -> assertBufferContains(buf, "front"))
                .verifyComplete();
    }

    @Test
    void streamCsvFileShouldEmitOnlyHeadersWhenNoFlashcardsExist() {
        when(flashcardRepository.findAll()).thenReturn(List.of());

        StepVerifier.create(flashcardService.streamCsvFile())
                .assertNext(buf -> assertBufferContains(buf, "#separator:tab"))
                .assertNext(buf -> assertBufferContains(buf, "#html:false"))
                .assertNext(buf -> assertBufferContains(buf, "#guid column:1"))
                .verifyComplete();
    }

    private void assertBufferContains(final DataBuffer buffer, final String expected) {
        final byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        assertThat(new String(bytes, StandardCharsets.UTF_8)).contains(expected);
    }

    @Test
    void saveShouldRecoverWhenConcurrentInsertCreatesReverseDeckFirst() {
        final DeckEntity deckWithoutReverse = new DeckEntity(1, "deck", null);
        final String reverseName = "deck_reversed";
        final DeckEntity winningReverseDeck = new DeckEntity(2, reverseName, deckWithoutReverse);

        final Flashcard flashcard = new Flashcard(
                null,
                deckWithoutReverse.getId(),
                deckWithoutReverse.getName(),
                "anki-1",
                "front",
                "back",
                "description",
                false
        );

        when(deckRepository.findById(deckWithoutReverse.getId())).thenReturn(Optional.of(deckWithoutReverse));
        when(deckRepository.findByName(reverseName))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winningReverseDeck));
        when(deckRepository.save(any(DeckEntity.class)))
                .thenAnswer(invocation -> {
                    final DeckEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
                    }
                    return entity;
                });

        final FlashcardEntity savedOriginal = new FlashcardEntity(
                10, deckWithoutReverse, null, "anki-1", "front", "back", "description", false);
        final FlashcardEntity savedReverse = new FlashcardEntity(
                11, winningReverseDeck, null, null, "back", "front", "description", false);

        when(flashcardRepository.save(any(FlashcardEntity.class)))
                .thenReturn(savedOriginal)
                .thenReturn(savedReverse);

        final FlashcardEntity result = flashcardService.save(flashcard);

        assertThat(result).isEqualTo(savedOriginal);
        assertThat(result.getReverseFlashcard()).isEqualTo(savedReverse);
        assertThat(savedReverse.getReverseFlashcard()).isEqualTo(savedOriginal);
    }

}
