package com.marvin.grocery.service;

import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.parser.ParsedItem;
import com.marvin.grocery.parser.ParsedReceipt;
import com.marvin.grocery.parser.ReceiptParserService;
import com.marvin.grocery.repository.ReceiptItemRepository;
import com.marvin.grocery.repository.ReceiptRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles transactional persistence of parsed receipts and their line items. */
@Service
public class ReceiptPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptPersistenceService.class);

    private final ReceiptParserService parserService;
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final ArticleService articleService;

    /**
     * Creates a new ReceiptPersistenceService with all required dependencies.
     *
     * @param parserService         the parser for structured item extraction
     * @param receiptRepository     the JPA repository for receipts
     * @param receiptItemRepository the JPA repository for receipt items
     * @param articleService        the service used to find or create the article per item
     */
    public ReceiptPersistenceService(
            ReceiptParserService parserService,
            ReceiptRepository receiptRepository,
            ReceiptItemRepository receiptItemRepository,
            ArticleService articleService) {
        this.parserService = parserService;
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.articleService = articleService;
    }

    /**
     * Parses the given OCR text, persists the receipt and all its line items in a single transaction,
     * and returns the UUID of the newly created receipt.
     *
     * @param ocrText the raw OCR text extracted from the image
     * @return the UUID of the saved receipt
     */
    @Transactional
    public UUID saveReceipt(String ocrText) {
        LOGGER.info("Parsing OCR text ({} chars):\n{}", ocrText.length(), ocrText);
        final ParsedReceipt parsed = parserService.parse(ocrText);
        LOGGER.info("Parsed {} items, receiptDate={}", parsed.items().size(), parsed.receiptDate());

        final ReceiptEntity receipt = new ReceiptEntity();
        receipt.setRawOcrText(ocrText);
        receipt.setReceiptDate(parsed.receiptDate());

        final ReceiptEntity saved = receiptRepository.save(receipt);
        LOGGER.info("Saved receipt {} with {} items", saved.getId(), parsed.items().size());

        for (final ParsedItem item : parsed.items()) {
            final ReceiptItemEntity itemEntity = new ReceiptItemEntity();
            itemEntity.setReceipt(saved);
            itemEntity.setArticle(articleService.findOrCreate(item.name()));
            itemEntity.setName(item.name());
            itemEntity.setSinglePrice(item.singlePrice());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setPrice(item.price());
            receiptItemRepository.save(itemEntity);
        }

        return saved.getId();
    }
}
