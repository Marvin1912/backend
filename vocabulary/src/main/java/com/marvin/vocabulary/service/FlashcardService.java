package com.marvin.vocabulary.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.marvin.vocabulary.dto.Flashcard;
import com.marvin.vocabulary.dto.FlashcardCsvDTO;
import com.marvin.vocabulary.model.DeckEntity;
import com.marvin.vocabulary.model.FlashcardEntity;
import com.marvin.vocabulary.repository.DeckRepository;
import com.marvin.vocabulary.repository.FlashcardRepository;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlashcardService {

    private final CsvSchema schema;
    private final FlashcardRepository flashcardRepository;
    private final DeckRepository deckRepository;

    public FlashcardService(FlashcardRepository flashcardRepository, DeckRepository deckRepository) {
        this.flashcardRepository = flashcardRepository;
        this.deckRepository = deckRepository;

        this.schema = CsvSchema.builder()
                .setColumnSeparator('\t')
                .setUseHeader(false)
                .setAllowComments(true)
                .addColumn("guid")
                .addColumn("deck")
                .addColumn("front")
                .addColumn("back")
                .addColumn("description")
                .disableQuoteChar()
                .build();
    }

    public FlashcardEntity get(int id) {
        return flashcardRepository.findById(id).orElse(null);
    }

    public List<FlashcardEntity> get(String missing, Boolean updated) {
        if (missing == null && updated == null) {
            return flashcardRepository.findAll();
        }

        if ("ankiId".equals(missing)) {
            return flashcardRepository.findByAnkiIdIsNull();
        }

        if (Boolean.TRUE.equals(updated)) {
            return flashcardRepository.findByUpdated(true);
        }

        return flashcardRepository.findAll();
    }

    @Transactional
    public FlashcardEntity save(Flashcard flashcard) {
        DeckEntity deck = getDeckOrThrow(flashcard.deckId());
        DeckEntity reverseDeck = getOrCreateReverseDeck(deck);

        FlashcardEntity original = new FlashcardEntity();
        original.setDeck(deck);
        original.setAnkiId(flashcard.ankiId());
        original.setFront(flashcard.front());
        original.setBack(flashcard.back());
        original.setDescription(flashcard.description());
        original.setUpdated(flashcard.updated());
        FlashcardEntity savedOriginal = flashcardRepository.save(original);

        FlashcardEntity reverse = new FlashcardEntity();
        reverse.setDeck(reverseDeck);
        reverse.setFront(flashcard.back());
        reverse.setBack(flashcard.front());
        reverse.setDescription(flashcard.description());
        reverse.setUpdated(flashcard.updated());
        FlashcardEntity savedReverse = flashcardRepository.save(reverse);

        savedOriginal.setReverseFlashcard(savedReverse);
        savedReverse.setReverseFlashcard(savedOriginal);

        return savedOriginal;
    }

    @Transactional
    public Integer update(Flashcard flashcard) {
        flashcardRepository.findById(flashcard.id())
                .ifPresentOrElse(
                        f -> {
                            DeckEntity deck = getDeckOrThrow(flashcard.deckId());
                            DeckEntity reverseDeck = getOrCreateReverseDeck(deck);
                            f.setDeck(deck);
                            f.setFront(flashcard.front());
                            f.setBack(flashcard.back());
                            f.setDescription(flashcard.description());
                            f.setAnkiId(flashcard.ankiId());
                            f.setUpdated(flashcard.updated());

                            FlashcardEntity reverse = f.getReverseFlashcard();
                            if (reverse == null) {
                                reverse = new FlashcardEntity();
                            }
                            reverse.setDeck(reverseDeck);
                            reverse.setFront(flashcard.back());
                            reverse.setBack(flashcard.front());
                            reverse.setDescription(flashcard.description());
                            reverse.setUpdated(flashcard.updated());
                            FlashcardEntity savedReverse = flashcardRepository.save(reverse);
                            f.setReverseFlashcard(savedReverse);
                            savedReverse.setReverseFlashcard(f);
                            flashcardRepository.save(savedReverse);
                        },
                        () -> {
                            throw new IllegalArgumentException(
                                    "No flashcard found with id: " + flashcard.id());
                        }
                );
        return flashcard.id();
    }

    @Transactional
    public Integer importFlashcards(byte[] fileBytes) {
        return importFile(fileBytes);
    }

    public byte[] getFile() throws Exception {

        final CsvMapper csvMapper = new CsvMapper();

        byte[] file;

        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                SequenceWriter sequenceWriter = csvMapper
                        .writerFor(FlashcardCsvDTO.class)
                        .with(schema)
                        .writeValues(byteArrayOutputStream)
        ) {

            byteArrayOutputStream.write("#separator:tab\n".getBytes(StandardCharsets.UTF_8));
            byteArrayOutputStream.write("#html:false\n".getBytes(StandardCharsets.UTF_8));
            byteArrayOutputStream.write("#guid column:1\n".getBytes(StandardCharsets.UTF_8));
            byteArrayOutputStream.flush();

            for (final FlashcardEntity flashcardEntity : flashcardRepository.findAll()) {
                sequenceWriter.write(new FlashcardCsvDTO(
                        flashcardEntity.getDeck().getName(),
                        flashcardEntity.getAnkiId(),
                        flashcardEntity.getFront(),
                        flashcardEntity.getBack(),
                        flashcardEntity.getDescription()
                ));
            }

            file = byteArrayOutputStream.toByteArray();

        }

        return file;
    }

    private int importFile(byte[] fileBytes) {

        final Set<String> allAnkiIds = flashcardRepository.getAllAnkiIds();

        final AtomicInteger count = new AtomicInteger(0);
        try (MappingIterator<FlashcardCsvDTO> iterator = new CsvMapper()
                .readerFor(FlashcardCsvDTO.class)
                .with(schema)
                .readValues(fileBytes)
        ) {
            for (final FlashcardCsvDTO flashcardCsvDto : iterator.readAll()) {
                try {
                    importFlashcard(allAnkiIds, flashcardCsvDto, count);
                } catch (Exception e) {
                    log.error("Failed to import flashcard {}", flashcardCsvDto, e);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return count.get();
    }

    private void importFlashcard(Set<String> allAnkiIds, FlashcardCsvDTO flashcardCsvDto,
            AtomicInteger count) {
        if (!allAnkiIds.contains(flashcardCsvDto.guid())) {
            flashcardRepository
                    .findByFrontAndBack(flashcardCsvDto.front(), flashcardCsvDto.back())
                    .ifPresentOrElse(
                            flashcard -> flashcard.setAnkiId(flashcardCsvDto.guid()),
                            () -> save(new Flashcard(
                                    null,
                                    getOrCreateDeck(flashcardCsvDto.deck()).getId(),
                                    flashcardCsvDto.deck(),
                                    flashcardCsvDto.guid(),
                                    flashcardCsvDto.front(),
                                    flashcardCsvDto.back(),
                                    flashcardCsvDto.description(),
                                    false
                            ))
                    );
            count.incrementAndGet();
        }
    }

    public Stream<Flashcard> getAllFlashcardsForExport() {
        return flashcardRepository.findAll().stream()
                .map(entity -> new Flashcard(
                        entity.getId(),
                        entity.getDeck().getId(),
                        entity.getDeck().getName(),
                        entity.getAnkiId(),
                        entity.getFront(),
                        entity.getBack(),
                        entity.getDescription(),
                        entity.isUpdated()
                ));
    }

    private DeckEntity getDeckOrThrow(Integer deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No deck found with id: " + deckId));
    }

    private DeckEntity getOrCreateDeck(String name) {
        return deckRepository.findByName(name)
                .orElseGet(() -> {
                    DeckEntity entity = new DeckEntity();
                    entity.setName(name);
                    return deckRepository.save(entity);
                });
    }

    private DeckEntity getOrCreateReverseDeck(DeckEntity deck) {
        if (deck.getReverseDeck() != null) {
            return deck.getReverseDeck();
        }

        String reverseName = deck.getName() + "_reversed";
        DeckEntity reverseDeck = deckRepository.findByName(reverseName)
                .orElseGet(() -> {
                    DeckEntity entity = new DeckEntity();
                    entity.setName(reverseName);
                    return deckRepository.save(entity);
                });
        deck.setReverseDeck(reverseDeck);
        reverseDeck.setReverseDeck(deck);
        deckRepository.save(deck);
        deckRepository.save(reverseDeck);
        return reverseDeck;
    }

}
