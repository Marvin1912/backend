package com.marvin.grocery.service;

import com.marvin.grocery.dto.ReceiptDTO;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.mapper.ReceiptMapper;
import com.marvin.grocery.ocr.OcrProvider;
import com.marvin.grocery.repository.ReceiptItemRepository;
import com.marvin.grocery.repository.ReceiptRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates OCR extraction, receipt parsing, and persistence. */
@Service
public class ReceiptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptService.class);

    private final OcrProvider ocrProvider;
    private final ReceiptPersistenceService persistenceService;
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final ReceiptMapper receiptMapper;

    /**
     * Creates a new ReceiptService with all required dependencies.
     *
     * @param ocrProvider        the OCR provider to use for text extraction
     * @param persistenceService the service responsible for transactional receipt persistence
     * @param receiptRepository  the JPA repository for receipts
     * @param receiptItemRepository the JPA repository for receipt items
     * @param receiptMapper      the MapStruct mapper for DTO conversion
     */
    public ReceiptService(
            OcrProvider ocrProvider,
            ReceiptPersistenceService persistenceService,
            ReceiptRepository receiptRepository,
            ReceiptItemRepository receiptItemRepository,
            ReceiptMapper receiptMapper) {
        this.ocrProvider = ocrProvider;
        this.persistenceService = persistenceService;
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.receiptMapper = receiptMapper;
    }

    /**
     * Processes an uploaded receipt image: runs OCR, parses items, and persists the result.
     *
     * @param imageBytes the raw bytes of the uploaded receipt image
     * @return a Mono emitting the UUID of the newly created receipt
     */
    public Mono<UUID> processAndSave(byte[] imageBytes) {
        LOGGER.info("Starting receipt processing for image of {} bytes", imageBytes.length);
        return ocrProvider.extractText(imageBytes)
                .doOnError(e -> LOGGER.error("OCR extraction failed", e))
                .flatMap(ocrText -> Mono.fromCallable(() -> persistenceService.saveReceipt(ocrText))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Returns all receipts without their item lists.
     *
     * @return a Flux emitting all stored receipts
     */
    public Flux<ReceiptDTO> findAll() {
        return Mono.fromCallable(() -> receiptRepository.findAllWithItems().stream()
                        .map(receiptMapper::toReceiptDTO)
                        .toList())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(list -> list);
    }

    /**
     * Returns the items for the given receipt, or empty if the receipt does not exist.
     *
     * @param receiptId the UUID of the receipt
     * @return a Mono emitting an optional list of item DTOs
     */
    public Mono<Optional<List<ReceiptItemDTO>>> findItems(UUID receiptId) {
        return Mono.fromCallable(() -> {
            if (!receiptRepository.existsById(receiptId)) {
                return Optional.<List<ReceiptItemDTO>>empty();
            }
            final List<ReceiptItemEntity> items = receiptItemRepository.findByReceiptId(receiptId);
            return Optional.of(receiptMapper.toReceiptItemDTOList(items));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
