package com.marvin.grocery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.PriceHistoryPointDTO;
import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.entity.Supermarket;
import com.marvin.grocery.repository.ReceiptItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceTrendService Tests")
class PriceTrendServiceTest {

    @Mock
    private ReceiptItemRepository receiptItemRepository;

    @InjectMocks
    private PriceTrendService priceTrendService;

    private static ReceiptEntity buildReceipt(LocalDate receiptDate, LocalDateTime creationDate, Supermarket supermarket) {
        final ReceiptEntity receipt = new ReceiptEntity();
        receipt.setId(UUID.randomUUID());
        receipt.setReceiptDate(receiptDate);
        receipt.setCreationDate(creationDate);
        receipt.setSupermarket(supermarket);
        return receipt;
    }

    private static ArticleGroupEntity buildGroup(Long id, String name) {
        final ArticleGroupEntity group = new ArticleGroupEntity();
        group.setId(id);
        group.setName(name);
        return group;
    }

    private static ArticleEntity buildArticle(Long id, String name, ArticleGroupEntity group) {
        final ArticleEntity article = new ArticleEntity();
        article.setId(id);
        article.setName(name);
        article.setArticleGroup(group);
        return article;
    }

    private static ReceiptItemEntity buildItem(
            Long id, ReceiptEntity receipt, ArticleEntity article, String name, BigDecimal singlePrice, int quantity) {
        final ReceiptItemEntity item = new ReceiptItemEntity();
        item.setId(id);
        item.setReceipt(receipt);
        item.setArticle(article);
        item.setName(name);
        item.setSinglePrice(singlePrice);
        item.setQuantity(quantity);
        item.setPrice(singlePrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    @Test
    @DisplayName("Should return empty Flux when there are no grouped receipt items")
    void findAllProductSummaries_EmptyRepository_ReturnsEmptyFlux() {
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of());

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should merge purchases of different articles belonging to the same group")
    void findAllProductSummaries_MultipleArticlesSameGroup_MergesAcrossArticles() {
        final ArticleGroupEntity group = buildGroup(1L, "Milch");
        final ArticleEntity vollmilch = buildArticle(10L, "Vollmilch", group);
        final ArticleEntity hMilch = buildArticle(11L, "H-Milch", group);
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.REWE);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, vollmilch, "Vollmilch", new BigDecimal("1.00"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, hMilch, "H-Milch", new BigDecimal("1.20"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> {
                    assertEquals(1L, summary.groupId());
                    assertEquals("Milch", summary.groupName());
                    assertEquals(2, summary.purchaseCount());
                    assertEquals(new BigDecimal("1.00"), summary.firstPrice());
                    assertEquals(new BigDecimal("1.20"), summary.latestPrice());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should exclude receipt items whose article has no group assignment")
    void findAllProductSummaries_ArticleWithoutGroup_IsExcluded() {
        final ArticleEntity ungroupedArticle = buildArticle(20L, "Sonderposten", null);
        final ReceiptEntity receipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity item = buildItem(1L, receipt, ungroupedArticle, "Sonderposten", new BigDecimal("2.00"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(item));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should exclude receipt items with no article at all")
    void findAllProductSummaries_ItemWithoutArticle_IsExcluded() {
        final ReceiptEntity receipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity item = buildItem(1L, receipt, null, "Unbekannt", new BigDecimal("2.00"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(item));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fall back to creationDate when receiptDate is null")
    void findAllProductSummaries_NullReceiptDate_FallsBackToCreationDate() {
        final ArticleGroupEntity group = buildGroup(1L, "Milchprodukte");
        final ArticleEntity article = buildArticle(10L, "Butter", group);
        final LocalDateTime creationDate = LocalDateTime.of(2026, 3, 5, 10, 30);
        final ReceiptEntity receipt = buildReceipt(null, creationDate, Supermarket.EDEKA);
        final ReceiptItemEntity item = buildItem(1L, receipt, article, "Butter", new BigDecimal("1.79"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(item));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> {
                    assertEquals(LocalDate.of(2026, 3, 5), summary.firstPurchaseDate());
                    assertEquals(LocalDate.of(2026, 3, 5), summary.latestPurchaseDate());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should compute percentChange from first to latest price")
    void findAllProductSummaries_ComputesPercentChange() {
        final ArticleGroupEntity group = buildGroup(1L, "Kaffee");
        final ArticleEntity article = buildArticle(10L, "Kaffee", group);
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, article, "Kaffee", new BigDecimal("1.00"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, article, "Kaffee", new BigDecimal("1.50"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> assertEquals(new BigDecimal("50.00"), summary.percentChange()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return null percentChange when the first price is zero")
    void findAllProductSummaries_ZeroFirstPrice_PercentChangeIsNull() {
        final ArticleGroupEntity group = buildGroup(1L, "Proben");
        final ArticleEntity article = buildArticle(10L, "Gratisprobe", group);
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, article, "Gratisprobe", BigDecimal.ZERO, 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, article, "Gratisprobe", new BigDecimal("0.99"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> assertNull(summary.percentChange()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should compute percentChange when the first price is negative (discount line)")
    void findAllProductSummaries_NegativeFirstPrice_ComputesPercentChange() {
        final ArticleGroupEntity group = buildGroup(1L, "Pfand");
        final ArticleEntity article = buildArticle(10L, "Pfandrueckgabe", group);
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, article, "Pfandrueckgabe", new BigDecimal("-0.50"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, article, "Pfandrueckgabe", new BigDecimal("1.00"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> assertEquals(new BigDecimal("-300.00"), summary.percentChange()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return zero percentChange for a group with a single purchase")
    void findAllProductSummaries_SinglePurchase_ZeroPercentChange() {
        final ArticleGroupEntity group = buildGroup(1L, "Suesswaren");
        final ArticleEntity article = buildArticle(10L, "Honig", group);
        final ReceiptEntity receipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity item = buildItem(1L, receipt, article, "Honig", new BigDecimal("3.49"), 1);
        when(receiptItemRepository.findAllGroupedWithReceipt()).thenReturn(List.of(item));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> {
                    assertEquals(new BigDecimal("0.00"), summary.percentChange());
                    assertEquals(1, summary.purchaseCount());
                    assertEquals(List.of(new BigDecimal("3.49")), summary.sparklinePrices());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return chronologically ordered history points for an article group")
    void findHistory_ReturnsChronologicallyOrderedPoints() {
        final ArticleGroupEntity group = buildGroup(1L, "Obst");
        final ArticleEntity article = buildArticle(10L, "Apfel", group);
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.REWE);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, article, "Apfel", new BigDecimal("0.59"), 3);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, article, "Apfel", new BigDecimal("0.49"), 2);
        when(receiptItemRepository.findAllByArticleGroupIdWithReceipt(eq(1L)))
                .thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findHistory(1L))
                .assertNext(history -> {
                    assertEquals(2, history.size());
                    final PriceHistoryPointDTO earliest = history.get(0);
                    final PriceHistoryPointDTO latest = history.get(1);
                    assertEquals(LocalDate.of(2026, 1, 1), earliest.date());
                    assertEquals(new BigDecimal("0.49"), earliest.singlePrice());
                    assertEquals(2, earliest.quantity());
                    assertEquals(Supermarket.LIDL, earliest.supermarket());
                    assertEquals(secondReceipt.getId(), earliest.receiptId());
                    assertEquals("Apfel", earliest.articleName());
                    assertEquals(LocalDate.of(2026, 2, 1), latest.date());
                    assertEquals(new BigDecimal("0.59"), latest.singlePrice());
                    assertEquals("Apfel", latest.articleName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should merge purchase history of different articles belonging to the same group")
    void findHistory_MergesAcrossArticlesInGroup() {
        final ArticleGroupEntity group = buildGroup(1L, "Milch");
        final ArticleEntity vollmilch = buildArticle(10L, "Vollmilch", group);
        final ArticleEntity hMilch = buildArticle(11L, "H-Milch", group);
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.REWE);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, vollmilch, "Vollmilch", new BigDecimal("1.09"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, hMilch, "H-Milch", new BigDecimal("1.19"), 1);
        when(receiptItemRepository.findAllByArticleGroupIdWithReceipt(eq(1L)))
                .thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findHistory(1L))
                .assertNext(history -> {
                    assertEquals(2, history.size());
                    assertEquals("Vollmilch", history.get(0).articleName());
                    assertEquals("H-Milch", history.get(1).articleName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should attribute supermarket and articleName per-point across multiple supermarkets and article variants")
    void findHistory_MultipleSupermarketsAndArticleVariants_AttributesPerPointCorrectly() {
        final ArticleGroupEntity group = buildGroup(1L, "Milch");
        final ArticleEntity vollmilch = buildArticle(10L, "Vollmilch", group);
        final ArticleEntity hMilch = buildArticle(11L, "H-Milch", group);
        final ArticleEntity biomilch = buildArticle(12L, "Bio-Milch", group);
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.REWE);
        final ReceiptEntity thirdReceipt = buildReceipt(LocalDate.of(2026, 3, 1), null, Supermarket.EDEKA);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, vollmilch, "Vollmilch", new BigDecimal("1.09"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, hMilch, "H-Milch", new BigDecimal("1.19"), 1);
        final ReceiptItemEntity thirdItem = buildItem(3L, thirdReceipt, biomilch, "Bio-Milch", new BigDecimal("1.49"), 1);
        when(receiptItemRepository.findAllByArticleGroupIdWithReceipt(eq(1L)))
                .thenReturn(List.of(firstItem, secondItem, thirdItem));

        StepVerifier.create(priceTrendService.findHistory(1L))
                .assertNext(history -> {
                    assertEquals(3, history.size());
                    final PriceHistoryPointDTO first = history.get(0);
                    final PriceHistoryPointDTO second = history.get(1);
                    final PriceHistoryPointDTO third = history.get(2);
                    assertEquals(Supermarket.LIDL, first.supermarket());
                    assertEquals("Vollmilch", first.articleName());
                    assertEquals(Supermarket.REWE, second.supermarket());
                    assertEquals("H-Milch", second.articleName());
                    assertEquals(Supermarket.EDEKA, third.supermarket());
                    assertEquals("Bio-Milch", third.articleName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should exclude history points whose article has no group assignment")
    void findHistory_ArticleWithoutGroup_IsExcluded() {
        final ArticleEntity ungroupedArticle = buildArticle(20L, "Sonderposten", null);
        final ReceiptEntity receipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity item = buildItem(1L, receipt, ungroupedArticle, "Sonderposten", new BigDecimal("2.00"), 1);
        when(receiptItemRepository.findAllByArticleGroupIdWithReceipt(eq(1L))).thenReturn(List.of(item));

        StepVerifier.create(priceTrendService.findHistory(1L))
                .assertNext(history -> assertEquals(0, history.size()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should exclude history points for receipt items with no article at all")
    void findHistory_ItemWithoutArticle_IsExcluded() {
        final ReceiptEntity receipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity item = buildItem(1L, receipt, null, "Unbekannt", new BigDecimal("2.00"), 1);
        when(receiptItemRepository.findAllByArticleGroupIdWithReceipt(eq(1L))).thenReturn(List.of(item));

        StepVerifier.create(priceTrendService.findHistory(1L))
                .assertNext(history -> assertEquals(0, history.size()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return an empty list when there is no purchase history for the group")
    void findHistory_NoMatches_ReturnsEmptyList() {
        when(receiptItemRepository.findAllByArticleGroupIdWithReceipt(eq(99L))).thenReturn(List.of());

        StepVerifier.create(priceTrendService.findHistory(99L))
                .assertNext(history -> assertEquals(0, history.size()))
                .verifyComplete();
    }
}
