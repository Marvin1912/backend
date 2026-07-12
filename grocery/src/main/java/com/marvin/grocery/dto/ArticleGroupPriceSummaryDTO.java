package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object representing the aggregated price trend for a single article group across all receipts.
 * Purchases are grouped by the {@code ArticleGroupEntity} that each receipt item's article is assigned to;
 * receipt items without an article, or whose article has no group assignment, are excluded.
 *
 * @param groupId             id of the article group
 * @param groupName           display name of the article group
 * @param firstPrice          single price at the chronologically first recorded purchase
 * @param firstPurchaseDate   date of the chronologically first recorded purchase
 * @param latestPrice         single price at the chronologically latest recorded purchase
 * @param latestPurchaseDate  date of the chronologically latest recorded purchase
 * @param percentChange       percentage change from firstPrice to latestPrice; null if firstPrice is zero
 * @param purchaseCount       number of recorded purchases within this article group
 * @param sparklinePrices     chronologically ordered single prices, for sparkline rendering
 */
@Schema(description = "Aggregated price trend summary for an article group across all receipts")
public record ArticleGroupPriceSummaryDTO(
        @Schema(description = "Id of the article group", example = "42")
        Long groupId,

        @Schema(description = "Display name of the article group", example = "Milch")
        String groupName,

        @Schema(description = "Single price at the chronologically first recorded purchase", example = "1.09")
        BigDecimal firstPrice,

        @Schema(description = "Date of the chronologically first recorded purchase")
        LocalDate firstPurchaseDate,

        @Schema(description = "Single price at the chronologically latest recorded purchase", example = "1.29")
        BigDecimal latestPrice,

        @Schema(description = "Date of the chronologically latest recorded purchase")
        LocalDate latestPurchaseDate,

        @Schema(description = "Percentage change from firstPrice to latestPrice; null if firstPrice is zero", example = "18.35")
        BigDecimal percentChange,

        @Schema(description = "Number of recorded purchases within this article group", example = "5")
        int purchaseCount,

        @Schema(description = "Chronologically ordered single prices, for sparkline rendering")
        List<BigDecimal> sparklinePrices
) {
}
