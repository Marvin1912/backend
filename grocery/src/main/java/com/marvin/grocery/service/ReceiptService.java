package com.marvin.grocery.service;

import com.marvin.grocery.dto.ReceiptDTO;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.mapper.ReceiptMapper;
import com.marvin.grocery.ocr.OcrProvider;
import com.marvin.grocery.parser.ParsedItem;
import com.marvin.grocery.parser.ParsedReceipt;
import com.marvin.grocery.parser.ReceiptParserService;
import com.marvin.grocery.repository.ReceiptItemRepository;
import com.marvin.grocery.repository.ReceiptRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates OCR extraction, receipt parsing, and persistence. */
@Service
public class ReceiptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptService.class);

    private final OcrProvider ocrProvider;
    private final ReceiptParserService parserService;
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final ReceiptMapper receiptMapper;

    /**
     * Creates a new ReceiptService with all required dependencies.
     *
     * @param ocrProvider           the OCR provider to use for text extraction
     * @param parserService         the parser for structured item extraction
     * @param receiptRepository     the JPA repository for receipts
     * @param receiptItemRepository the JPA repository for receipt items
     * @param receiptMapper         the MapStruct mapper for DTO conversion
     */
    public ReceiptService(
            OcrProvider ocrProvider,
            ReceiptParserService parserService,
            ReceiptRepository receiptRepository,
            ReceiptItemRepository receiptItemRepository,
            ReceiptMapper receiptMapper) {
        this.ocrProvider = ocrProvider;
        this.parserService = parserService;
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
        return ocrProvider.extractText(imageBytes)
                .flatMap(ocrText -> Mono.fromCallable(() -> saveReceipt(imageBytes, ocrText))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Returns all receipts without their item lists.
     *
     * @return a Flux emitting all stored receipts
     */
    public Flux<ReceiptDTO> findAll() {
        return Flux.fromIterable(receiptRepository.findAll())
                .map(receiptMapper::toReceiptDTO);
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
     * Saves the receipt and its parsed items transactionally.
     *
     * @param imageBytes raw image bytes to store
     * @param ocrText    the raw OCR text extracted from the image
     * @return the UUID of the saved receipt
     */
    @Transactional
    private UUID saveReceipt(byte[] imageBytes, String ocrText) {
        final ParsedReceipt parsed = parserService.parse(ocrText);

        final ReceiptEntity receipt = new ReceiptEntity();
        receipt.setImageContent(imageBytes);
        receipt.setRawOcrText(ocrText);
        receipt.setReceiptDate(parsed.receiptDate());

        final ReceiptEntity saved = receiptRepository.save(receipt);
        LOGGER.debug("Saved receipt {} with {} items", saved.getId(), parsed.items().size());

        for (final ParsedItem item : parsed.items()) {
            final ReceiptItemEntity itemEntity = new ReceiptItemEntity();
            itemEntity.setReceipt(saved);
            itemEntity.setName(item.name());
            itemEntity.setPrice(item.price());
            receiptItemRepository.save(itemEntity);
        }

        return saved.getId();
    }
}
