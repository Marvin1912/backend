package com.marvin.grocery.service;

import com.marvin.grocery.dto.PriceHistoryPointDTO;
import com.marvin.grocery.dto.ProductPriceSummaryDTO;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.repository.ReceiptItemRepository;
import com.marvin.grocery.util.ArticleNameNormalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Computes read-only price-trend aggregations over existing receipt and receipt item data.
 * Products are matched across receipts by exact name match, case-insensitive and whitespace-trimmed.
 * Aggregation happens in memory rather than via SQL {@code GROUP BY}, since chronological first/latest
 * price cannot be expressed safely with {@code MIN}/{@code MAX} alone.
 */
@Service
public class PriceTrendService {

    private static final Comparator<ReceiptItemEntity> CHRONOLOGICAL_ORDER =
            Comparator.comparing(PriceTrendService::effectiveDate).thenComparing(ReceiptItemEntity::getId);
    private static final int PERCENT_CHANGE_SCALE = 2;

    private final ReceiptItemRepository receiptItemRepository;

    /**
     * Creates a new PriceTrendService with the required repository.
     *
     * @param receiptItemRepository the JPA repository for receipt items
     */
    public PriceTrendService(ReceiptItemRepository receiptItemRepository) {
        this.receiptItemRepository = receiptItemRepository;
    }

    /**
     * Returns a price trend summary for every distinct product, grouped by normalized name.
     *
     * @return a Flux emitting one summary per distinct product, ordered by display name
     */
    public Flux<ProductPriceSummaryDTO> findAllProductSummaries() {
        return Mono.fromCallable(this::computeSummaries)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(summaries -> summaries);
    }

    /**
     * Returns the chronologically ordered purchase history for a single product.
     *
     * @param name the product name; matched case-insensitively and whitespace-trimmed
     * @return a Mono emitting the ordered list of history points, empty if the product was never purchased
     */
    public Mono<List<PriceHistoryPointDTO>> findHistory(String name) {
        final String normalizedName = ArticleNameNormalizer.normalize(name);
        return Mono.fromCallable(() -> receiptItemRepository.findAllByNormalizedNameWithReceipt(normalizedName).stream()
                        .sorted(CHRONOLOGICAL_ORDER)
                        .map(PriceTrendService::toHistoryPoint)
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private List<ProductPriceSummaryDTO> computeSummaries() {
        final Map<String, List<ReceiptItemEntity>> itemsByNormalizedName = receiptItemRepository.findAllWithReceipt()
                .stream()
                .collect(Collectors.groupingBy(item -> ArticleNameNormalizer.normalize(item.getName())));
        return itemsByNormalizedName.values().stream()
                .map(PriceTrendService::toSummary)
                .sorted(Comparator.comparing(ProductPriceSummaryDTO::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static ProductPriceSummaryDTO toSummary(List<ReceiptItemEntity> productItems) {
        final List<ReceiptItemEntity> chronological = productItems.stream().sorted(CHRONOLOGICAL_ORDER).toList();
        final ReceiptItemEntity first = chronological.get(0);
        final ReceiptItemEntity latest = chronological.get(chronological.size() - 1);
        final List<BigDecimal> sparklinePrices = chronological.stream().map(ReceiptItemEntity::getSinglePrice).toList();
        return new ProductPriceSummaryDTO(
                latest.getName().trim(),
                ArticleNameNormalizer.normalize(latest.getName()),
                first.getSinglePrice(),
                effectiveDate(first),
                latest.getSinglePrice(),
                effectiveDate(latest),
                computePercentChange(first.getSinglePrice(), latest.getSinglePrice()),
                chronological.size(),
                sparklinePrices
        );
    }

    private static PriceHistoryPointDTO toHistoryPoint(ReceiptItemEntity item) {
        return new PriceHistoryPointDTO(
                effectiveDate(item),
                item.getSinglePrice(),
                item.getQuantity(),
                item.getReceipt().getSupermarket(),
                item.getReceipt().getId()
        );
    }

    private static BigDecimal computePercentChange(BigDecimal firstPrice, BigDecimal latestPrice) {
        if (firstPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return latestPrice.subtract(firstPrice)
                .divide(firstPrice, PERCENT_CHANGE_SCALE + 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PERCENT_CHANGE_SCALE, RoundingMode.HALF_UP);
    }

    private static LocalDate effectiveDate(ReceiptItemEntity item) {
        final ReceiptEntity receipt = item.getReceipt();
        return receipt.getReceiptDate() != null ? receipt.getReceiptDate() : receipt.getCreationDate().toLocalDate();
    }
}
