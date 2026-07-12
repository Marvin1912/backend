package com.marvin.grocery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.parser.ParsedItem;
import com.marvin.grocery.parser.ParsedReceipt;
import com.marvin.grocery.parser.ReceiptParserService;
import com.marvin.grocery.repository.ReceiptItemRepository;
import com.marvin.grocery.repository.ReceiptRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptPersistenceService Tests")
class ReceiptPersistenceServiceTest {

    @Mock
    private ReceiptParserService parserService;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptItemRepository receiptItemRepository;

    @Mock
    private ArticleService articleService;

    @InjectMocks
    private ReceiptPersistenceService receiptPersistenceService;

    @Test
    @DisplayName("Should link a parsed item to the article resolved via find-or-create")
    void saveReceipt_ParsedItem_LinksResolvedArticle() {
        final String ocrText = "raw ocr text";
        final ParsedItem parsedItem = new ParsedItem("Milch", new BigDecimal("1.09"), 1, new BigDecimal("1.09"));
        when(parserService.parse(ocrText)).thenReturn(new ParsedReceipt(List.of(parsedItem), LocalDate.of(2026, 1, 1)));
        final ReceiptEntity savedReceipt = new ReceiptEntity();
        savedReceipt.setId(UUID.randomUUID());
        when(receiptRepository.save(any(ReceiptEntity.class))).thenReturn(savedReceipt);
        final ArticleEntity resolvedArticle = new ArticleEntity();
        resolvedArticle.setId(42L);
        resolvedArticle.setName("Milch");
        resolvedArticle.setNormalizedName("milch");
        when(articleService.findOrCreate("Milch")).thenReturn(resolvedArticle);
        when(receiptItemRepository.save(any(ReceiptItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        receiptPersistenceService.saveReceipt(ocrText);

        final ArgumentCaptor<ReceiptItemEntity> itemCaptor = ArgumentCaptor.forClass(ReceiptItemEntity.class);
        verify(receiptItemRepository).save(itemCaptor.capture());
        final ReceiptItemEntity savedItem = itemCaptor.getValue();
        assertEquals("Milch", savedItem.getName());
        assertSame(resolvedArticle, savedItem.getArticle());
    }

    @Test
    @DisplayName("Should resolve an article per parsed item via find-or-create")
    void saveReceipt_MultipleParsedItems_ResolvesArticlePerItem() {
        final String ocrText = "raw ocr text";
        final ParsedItem firstItem = new ParsedItem("Milch", new BigDecimal("1.09"), 1, new BigDecimal("1.09"));
        final ParsedItem secondItem = new ParsedItem("Butter", new BigDecimal("1.79"), 1, new BigDecimal("1.79"));
        when(parserService.parse(ocrText)).thenReturn(new ParsedReceipt(List.of(firstItem, secondItem), null));
        final ReceiptEntity savedReceipt = new ReceiptEntity();
        savedReceipt.setId(UUID.randomUUID());
        when(receiptRepository.save(any(ReceiptEntity.class))).thenReturn(savedReceipt);
        when(articleService.findOrCreate(any(String.class))).thenReturn(new ArticleEntity());
        when(receiptItemRepository.save(any(ReceiptItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        receiptPersistenceService.saveReceipt(ocrText);

        verify(articleService, times(1)).findOrCreate("Milch");
        verify(articleService, times(1)).findOrCreate("Butter");
        verify(receiptItemRepository, times(2)).save(any(ReceiptItemEntity.class));
    }
}
