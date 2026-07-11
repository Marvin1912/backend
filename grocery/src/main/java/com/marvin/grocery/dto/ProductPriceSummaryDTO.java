package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object representing the aggregated price trend for a single product across all receipts.
 * Products are matched across receipts by exact name match, case-insensitive and whitespace-trimmed.
 *
 * @param name              display name of the product, taken from the most recent purchase
 * @param normalizedName    lower-cased, trimmed name used to group purchases of the same product
 * @param firstPrice        single price at the chronologically first recorded purchase
 * @param firstPurchaseDate date of the chronologically first recorded purchase
 * @param latestPrice       single price at the chronologically latest recorded purchase
 * @param latestPurchaseDate date of the chronologically latest recorded purchase
 * @param percentChange     percentage change from firstPrice to latestPrice; null if firstPrice is zero
 * @param purchaseCount     number of recorded purchases of this product
 * @param sparklinePrices   chronologically ordered single prices, for sparkline rendering
 */
@Schema(description = "Aggregated price trend summary for a product across all receipts")
public record ProductPriceSummaryDTO(
        @Schema(description = "Display name of the product, taken from the most recent purchase", example = "Vollmilch 3,5%")
        String name,

        @Schema(description = "Normalized (lower-cased, trimmed) name used to group purchases of the same product",
                example = "vollmilch 3,5%")
        String normalizedName,

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

        @Schema(description = "Number of recorded purchases of this product", example = "5")
        int purchaseCount,

        @Schema(description = "Chronologically ordered single prices, for sparkline rendering")
        List<BigDecimal> sparklinePrices
) {
}
