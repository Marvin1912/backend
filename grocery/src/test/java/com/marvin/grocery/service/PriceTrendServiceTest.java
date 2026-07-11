package com.marvin.grocery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.PriceHistoryPointDTO;
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

    private static ReceiptItemEntity buildItem(Long id, ReceiptEntity receipt, String name, BigDecimal singlePrice, int quantity) {
        final ReceiptItemEntity item = new ReceiptItemEntity();
        item.setId(id);
        item.setReceipt(receipt);
        item.setName(name);
        item.setSinglePrice(singlePrice);
        item.setQuantity(quantity);
        item.setPrice(singlePrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    @Test
    @DisplayName("Should return empty Flux when there are no receipt items")
    void findAllProductSummaries_EmptyRepository_ReturnsEmptyFlux() {
        when(receiptItemRepository.findAllWithReceipt()).thenReturn(List.of());

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should merge purchases whose names differ only by case and whitespace")
    void findAllProductSummaries_NameNormalization_MergesCaseAndWhitespaceVariants() {
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.REWE);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, "Milch", new BigDecimal("1.00"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, " milch ", new BigDecimal("1.20"), 1);
        when(receiptItemRepository.findAllWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> {
                    assertEquals("milch", summary.normalizedName());
                    assertEquals(2, summary.purchaseCount());
                    assertEquals(new BigDecimal("1.00"), summary.firstPrice());
                    assertEquals(new BigDecimal("1.20"), summary.latestPrice());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fall back to creationDate when receiptDate is null")
    void findAllProductSummaries_NullReceiptDate_FallsBackToCreationDate() {
        final LocalDateTime creationDate = LocalDateTime.of(2026, 3, 5, 10, 30);
        final ReceiptEntity receipt = buildReceipt(null, creationDate, Supermarket.EDEKA);
        final ReceiptItemEntity item = buildItem(1L, receipt, "Butter", new BigDecimal("1.79"), 1);
        when(receiptItemRepository.findAllWithReceipt()).thenReturn(List.of(item));

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
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, "Kaffee", new BigDecimal("1.00"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, "Kaffee", new BigDecimal("1.50"), 1);
        when(receiptItemRepository.findAllWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> assertEquals(new BigDecimal("50.00"), summary.percentChange()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return null percentChange when the first price is zero")
    void findAllProductSummaries_ZeroFirstPrice_PercentChangeIsNull() {
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, "Gratisprobe", BigDecimal.ZERO, 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, "Gratisprobe", new BigDecimal("0.99"), 1);
        when(receiptItemRepository.findAllWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> assertNull(summary.percentChange()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should compute percentChange when the first price is negative (discount line)")
    void findAllProductSummaries_NegativeFirstPrice_ComputesPercentChange() {
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, "Pfandrueckgabe", new BigDecimal("-0.50"), 1);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, "Pfandrueckgabe", new BigDecimal("1.00"), 1);
        when(receiptItemRepository.findAllWithReceipt()).thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> assertEquals(new BigDecimal("-300.00"), summary.percentChange()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return zero percentChange for a product with a single purchase")
    void findAllProductSummaries_SinglePurchase_ZeroPercentChange() {
        final ReceiptEntity receipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity item = buildItem(1L, receipt, "Honig", new BigDecimal("3.49"), 1);
        when(receiptItemRepository.findAllWithReceipt()).thenReturn(List.of(item));

        StepVerifier.create(priceTrendService.findAllProductSummaries())
                .assertNext(summary -> {
                    assertEquals(new BigDecimal("0.00"), summary.percentChange());
                    assertEquals(1, summary.purchaseCount());
                    assertEquals(List.of(new BigDecimal("3.49")), summary.sparklinePrices());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return chronologically ordered history points for a product")
    void findHistory_ReturnsChronologicallyOrderedPoints() {
        final ReceiptEntity firstReceipt = buildReceipt(LocalDate.of(2026, 2, 1), null, Supermarket.REWE);
        final ReceiptEntity secondReceipt = buildReceipt(LocalDate.of(2026, 1, 1), null, Supermarket.LIDL);
        final ReceiptItemEntity firstItem = buildItem(1L, firstReceipt, "Apfel", new BigDecimal("0.59"), 3);
        final ReceiptItemEntity secondItem = buildItem(2L, secondReceipt, "Apfel", new BigDecimal("0.49"), 2);
        when(receiptItemRepository.findAllByNormalizedNameWithReceipt(eq("apfel")))
                .thenReturn(List.of(firstItem, secondItem));

        StepVerifier.create(priceTrendService.findHistory("Apfel"))
                .assertNext(history -> {
                    assertEquals(2, history.size());
                    final PriceHistoryPointDTO earliest = history.get(0);
                    final PriceHistoryPointDTO latest = history.get(1);
                    assertEquals(LocalDate.of(2026, 1, 1), earliest.date());
                    assertEquals(new BigDecimal("0.49"), earliest.singlePrice());
                    assertEquals(2, earliest.quantity());
                    assertEquals(Supermarket.LIDL, earliest.supermarket());
                    assertEquals(secondReceipt.getId(), earliest.receiptId());
                    assertEquals(LocalDate.of(2026, 2, 1), latest.date());
                    assertEquals(new BigDecimal("0.59"), latest.singlePrice());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should normalize the name before looking up history")
    void findHistory_NormalizesNameForLookup() {
        when(receiptItemRepository.findAllByNormalizedNameWithReceipt(eq("milch"))).thenReturn(List.of());

        StepVerifier.create(priceTrendService.findHistory(" Milch "))
                .assertNext(history -> assertEquals(List.of(), history))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return an empty list when there is no purchase history for the product")
    void findHistory_NoMatches_ReturnsEmptyList() {
        when(receiptItemRepository.findAllByNormalizedNameWithReceipt(eq("unbekannt"))).thenReturn(List.of());

        StepVerifier.create(priceTrendService.findHistory("unbekannt"))
                .assertNext(history -> assertEquals(0, history.size()))
                .verifyComplete();
    }
}
