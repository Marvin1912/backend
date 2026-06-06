package com.marvin.grocery.service;

import com.marvin.grocery.dto.AddReceiptItemRequest;
import com.marvin.grocery.dto.ReceiptDTO;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.dto.UpdateReceiptItemRequest;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.entity.Supermarket;
import com.marvin.grocery.mapper.ReceiptMapper;
import com.marvin.grocery.ocr.OcrProvider;
import com.marvin.grocery.repository.ReceiptItemRepository;
import com.marvin.grocery.repository.ReceiptRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
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

    /**
     * Manually adds a new item to the given receipt and persists it.
     * Emits {@link NoSuchElementException} if the receipt does not exist.
     *
     * @param receiptId the UUID of the parent receipt
     * @param request   the new item fields
     * @return a Mono emitting the created item DTO
     */
    public Mono<ReceiptItemDTO> addItem(UUID receiptId, AddReceiptItemRequest request) {
        return Mono.fromCallable(() -> {
            final ReceiptEntity receipt = receiptRepository.findById(receiptId)
                    .orElseThrow(() -> new NoSuchElementException("Receipt not found: " + receiptId));
            final ReceiptItemEntity item = new ReceiptItemEntity();
            item.setReceipt(receipt);
            item.setName(request.name());
            item.setQuantity(request.quantity());
            item.setSinglePrice(request.singlePrice());
            item.setPrice(request.singlePrice().multiply(BigDecimal.valueOf(request.quantity())));
            return receiptMapper.toReceiptItemDTO(receiptItemRepository.save(item));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates the editable fields of a receipt item and recalculates its total price.
     * Emits {@link NoSuchElementException} if the item does not exist or does not belong to the receipt.
     *
     * @param receiptId the UUID of the parent receipt
     * @param itemId    the id of the item to update
     * @param request   the new field values
     * @return a Mono emitting the updated item DTO
     */
    public Mono<ReceiptItemDTO> updateItem(UUID receiptId, Long itemId, UpdateReceiptItemRequest request) {
        return Mono.fromCallable(() -> {
            final ReceiptItemEntity item = receiptItemRepository.findByIdAndReceiptId(itemId, receiptId)
                    .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));
            item.setName(request.name());
            item.setQuantity(request.quantity());
            item.setSinglePrice(request.singlePrice());
            item.setPrice(request.singlePrice().multiply(BigDecimal.valueOf(request.quantity())));
            return receiptMapper.toReceiptItemDTO(receiptItemRepository.save(item));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates the supermarket field on the given receipt.
     * Emits {@link NoSuchElementException} if the receipt does not exist.
     *
     * @param receiptId   the UUID of the receipt to update
     * @param supermarket the supermarket to set
     * @return a Mono emitting the updated receipt DTO
     */
    public Mono<ReceiptDTO> updateSupermarket(UUID receiptId, Supermarket supermarket) {
        return Mono.fromCallable(() -> {
            final ReceiptEntity receipt = receiptRepository.findById(receiptId)
                    .orElseThrow(() -> new NoSuchElementException("Receipt not found: " + receiptId));
            receipt.setSupermarket(supermarket);
            final ReceiptEntity saved = receiptRepository.save(receipt);
            return receiptMapper.toReceiptDTO(saved);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes the receipt with the given id and all its associated items.
     * Emits {@link NoSuchElementException} if no receipt with that id exists.
     *
     * @param id the UUID of the receipt to delete
     * @return an empty Mono on success, or an error Mono if the receipt does not exist
     */
    public Mono<Void> deleteReceipt(UUID id) {
        return Mono.fromCallable(() -> {
            if (!receiptRepository.existsById(id)) {
                throw new NoSuchElementException("Receipt not found: " + id);
            }
            receiptRepository.deleteById(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
