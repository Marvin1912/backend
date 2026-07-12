package com.marvin.grocery.service;

import com.marvin.grocery.dto.ArticleGroupPriceSummaryDTO;
import com.marvin.grocery.dto.PriceHistoryPointDTO;
import com.marvin.grocery.entity.ArticleGroupEntity;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import com.marvin.grocery.repository.ReceiptItemRepository;
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
 * Purchases are grouped by the {@link ArticleGroupEntity} that each receipt item's article is assigned to.
 * Receipt items without an article, or whose article has no group assignment, are excluded entirely until
 * the article is manually assigned to a group.
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
     * Returns a price trend summary for every article group with at least one recorded purchase.
     * Receipt items without an article, or whose article has no group assignment, are excluded.
     *
     * @return a Flux emitting one summary per article group, ordered by group name
     */
    public Flux<ArticleGroupPriceSummaryDTO> findAllProductSummaries() {
        return Mono.fromCallable(this::computeSummaries)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(summaries -> summaries);
    }

    /**
     * Returns the chronologically ordered purchase history for a single article group, merged across
     * every article currently assigned to that group.
     *
     * @param groupId the id of the article group
     * @return a Mono emitting the ordered list of history points, empty if the group has no purchases
     */
    public Mono<List<PriceHistoryPointDTO>> findHistory(Long groupId) {
        return Mono.fromCallable(() -> receiptItemRepository.findAllByArticleGroupIdWithReceipt(groupId).stream()
                        .filter(PriceTrendService::hasArticleGroup)
                        .sorted(CHRONOLOGICAL_ORDER)
                        .map(PriceTrendService::toHistoryPoint)
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private List<ArticleGroupPriceSummaryDTO> computeSummaries() {
        final Map<Long, List<ReceiptItemEntity>> itemsByGroupId = receiptItemRepository.findAllGroupedWithReceipt()
                .stream()
                .filter(PriceTrendService::hasArticleGroup)
                .collect(Collectors.groupingBy(item -> item.getArticle().getArticleGroup().getId()));
        return itemsByGroupId.values().stream()
                .map(PriceTrendService::toSummary)
                .sorted(Comparator.comparing(ArticleGroupPriceSummaryDTO::groupName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static boolean hasArticleGroup(ReceiptItemEntity item) {
        return item.getArticle() != null && item.getArticle().getArticleGroup() != null;
    }

    private static ArticleGroupPriceSummaryDTO toSummary(List<ReceiptItemEntity> groupItems) {
        final List<ReceiptItemEntity> chronological = groupItems.stream().sorted(CHRONOLOGICAL_ORDER).toList();
        final ReceiptItemEntity first = chronological.get(0);
        final ReceiptItemEntity latest = chronological.get(chronological.size() - 1);
        final ArticleGroupEntity group = latest.getArticle().getArticleGroup();
        final List<BigDecimal> sparklinePrices = chronological.stream().map(ReceiptItemEntity::getSinglePrice).toList();
        return new ArticleGroupPriceSummaryDTO(
                group.getId(),
                group.getName(),
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
                item.getReceipt().getId(),
                item.getArticle().getName()
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
