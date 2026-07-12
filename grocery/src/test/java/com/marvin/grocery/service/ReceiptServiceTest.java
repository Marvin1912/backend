package com.marvin.grocery.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.AddReceiptItemRequest;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.dto.UpdateReceiptItemRequest;
import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.mapper.ReceiptMapper;
import com.marvin.grocery.ocr.OcrProvider;
import com.marvin.grocery.repository.ReceiptItemRepository;
import com.marvin.grocery.repository.ReceiptRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService Tests")
class ReceiptServiceTest {

    @Mock
    private OcrProvider ocrProvider;

    @Mock
    private ReceiptPersistenceService persistenceService;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptItemRepository receiptItemRepository;

    @Mock
    private ReceiptMapper receiptMapper;

    @Mock
    private ArticleService articleService;

    @InjectMocks
    private ReceiptService receiptService;

    @Test
    @DisplayName("addItem should link the article resolved via find-or-create for the item's name")
    void addItem_ResolvesArticleAndLinksIt() {
        final UUID receiptId = UUID.randomUUID();
        final ReceiptEntity receipt = new ReceiptEntity();
        receipt.setId(receiptId);
        final AddReceiptItemRequest request = new AddReceiptItemRequest("Milch", 2, new BigDecimal("1.09"));
        final ArticleEntity resolvedArticle = new ArticleEntity();
        resolvedArticle.setId(7L);
        resolvedArticle.setName("Milch");
        resolvedArticle.setNormalizedName("milch");
        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(articleService.findOrCreate("Milch")).thenReturn(resolvedArticle);
        when(receiptItemRepository.save(any(ReceiptItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptMapper.toReceiptItemDTO(any(ReceiptItemEntity.class)))
                .thenReturn(new ReceiptItemDTO(1L, "Milch", new BigDecimal("1.09"), 2, new BigDecimal("2.18")));

        StepVerifier.create(receiptService.addItem(receiptId, request))
                .expectNextCount(1)
                .verifyComplete();

        final ArgumentCaptor<ReceiptItemEntity> itemCaptor = ArgumentCaptor.forClass(ReceiptItemEntity.class);
        verify(receiptItemRepository).save(itemCaptor.capture());
        assertSame(resolvedArticle, itemCaptor.getValue().getArticle());
    }

    @Test
    @DisplayName("updateItem should re-resolve the article via find-or-create for the updated name")
    void updateItem_ResolvesArticleAndLinksIt() {
        final UUID receiptId = UUID.randomUUID();
        final Long itemId = 5L;
        final ReceiptItemEntity existingItem = new ReceiptItemEntity();
        existingItem.setId(itemId);
        final UpdateReceiptItemRequest request = new UpdateReceiptItemRequest("Kaffee", 1, new BigDecimal("3.49"));
        final ArticleEntity resolvedArticle = new ArticleEntity();
        resolvedArticle.setId(9L);
        resolvedArticle.setName("Kaffee");
        resolvedArticle.setNormalizedName("kaffee");
        when(receiptItemRepository.findByIdAndReceiptId(itemId, receiptId)).thenReturn(Optional.of(existingItem));
        when(articleService.findOrCreate("Kaffee")).thenReturn(resolvedArticle);
        when(receiptItemRepository.save(any(ReceiptItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptMapper.toReceiptItemDTO(any(ReceiptItemEntity.class)))
                .thenReturn(new ReceiptItemDTO(itemId, "Kaffee", new BigDecimal("3.49"), 1, new BigDecimal("3.49")));

        StepVerifier.create(receiptService.updateItem(receiptId, itemId, request))
                .expectNextCount(1)
                .verifyComplete();

        assertSame(resolvedArticle, existingItem.getArticle());
    }
}
