package com.marvin.vocabulary.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.marvin.vocabulary.dto.Flashcard;
import com.marvin.vocabulary.dto.FlashcardCsvDTO;
import com.marvin.vocabulary.model.DeckEntity;
import com.marvin.vocabulary.model.FlashcardEntity;
import com.marvin.vocabulary.repository.DeckRepository;
import com.marvin.vocabulary.repository.FlashcardRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Service for managing vocabulary flashcards, including CRUD operations, CSV import, and streaming CSV export.
 */
@Slf4j
@Service
public class FlashcardService {

    private final CsvSchema schema;
    private final FlashcardRepository flashcardRepository;
    private final DeckRepository deckRepository;
    private final DataBufferFactory dataBufferFactory;

    /**
     * Constructs a new FlashcardService with required dependencies.
     *
     * @param flashcardRepository the flashcard repository
     * @param deckRepository      the deck repository
     */
    public FlashcardService(FlashcardRepository flashcardRepository, DeckRepository deckRepository) {
        this.flashcardRepository = flashcardRepository;
        this.deckRepository = deckRepository;
        this.dataBufferFactory = new DefaultDataBufferFactory();

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

    /**
     * Retrieves a flashcard by its ID.
     *
     * @param id the flashcard ID
     * @return the flashcard entity, or {@code null} if not found
     */
    public FlashcardEntity get(int id) {
        return flashcardRepository.findById(id).orElse(null);
    }

    /**
     * Retrieves flashcards with optional filtering.
     *
     * @param missing optional filter name (e.g. {@code "ankiId"})
     * @param updated optional filter for updated status
     * @return a list of matching flashcard entities
     */
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

    /**
     * Saves a new flashcard and its reverse counterpart.
     *
     * @param flashcard the flashcard data to persist
     * @return the saved original flashcard entity
     */
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

    /**
     * Updates an existing flashcard and its reverse counterpart.
     *
     * @param flashcard the flashcard data to update
     * @return the ID of the updated flashcard
     */
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

    /**
     * Imports flashcards from a CSV file provided as a byte array.
     *
     * @param fileBytes the raw bytes of the CSV file
     * @return the number of flashcards successfully imported
     */
    @Transactional
    public Integer importFlashcards(byte[] fileBytes) {
        return importFile(fileBytes);
    }

    /**
     * Streams all flashcards as CSV-formatted {@link DataBuffer} elements.
     *
     * <p>The response begins with Anki-compatible header lines, followed by one tab-separated row
     * per flashcard entity. The blocking JPA {@code findAll()} call is offloaded to the bounded
     * elastic scheduler so the event loop thread is never blocked.</p>
     *
     * @return a {@link Flux} of {@link DataBuffer} chunks representing the CSV content
     */
    public Flux<DataBuffer> streamCsvFile() {
        final ObjectWriter csvWriter = new CsvMapper()
                .writerFor(FlashcardCsvDTO.class)
                .with(schema);

        final Flux<DataBuffer> header = Flux.just(
                dataBufferFactory.wrap("#separator:tab\n".getBytes(StandardCharsets.UTF_8)),
                dataBufferFactory.wrap("#html:false\n".getBytes(StandardCharsets.UTF_8)),
                dataBufferFactory.wrap("#guid column:1\n".getBytes(StandardCharsets.UTF_8))
        );

        final Flux<DataBuffer> rows = Flux
                .defer(() -> Flux.fromIterable(flashcardRepository.findAll()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(entity -> new FlashcardCsvDTO(
                        entity.getDeck().getName(),
                        entity.getAnkiId(),
                        entity.getFront(),
                        entity.getBack(),
                        entity.getDescription()
                ))
                .map(dto -> {
                    try {
                        final byte[] rowBytes = csvWriter.writeValueAsBytes(dto);
                        return dataBufferFactory.wrap(rowBytes);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to serialize flashcard to CSV", e);
                    }
                });

        return Flux.concat(header, rows);
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

    /**
     * Returns a stream of all flashcards mapped to DTOs for export purposes.
     *
     * @return a stream of {@link Flashcard} DTOs
     */
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
        return findOrCreateDeckByName(name);
    }

    private DeckEntity getOrCreateReverseDeck(DeckEntity deck) {
        if (deck.getReverseDeck() != null) {
            return deck.getReverseDeck();
        }

        String reverseName = deck.getName() + "_reversed";
        DeckEntity reverseDeck = findOrCreateDeckByName(reverseName);
        deck.setReverseDeck(reverseDeck);
        reverseDeck.setReverseDeck(deck);
        deckRepository.save(deck);
        deckRepository.save(reverseDeck);
        return reverseDeck;
    }

    private DeckEntity findOrCreateDeckByName(String name) {
        return deckRepository.findByName(name)
                .orElseGet(() -> {
                    DeckEntity entity = new DeckEntity();
                    entity.setName(name);
                    try {
                        return deckRepository.save(entity);
                    } catch (DataIntegrityViolationException e) {
                        return deckRepository.findByName(name).orElseThrow(() -> e);
                    }
                });
    }

}
