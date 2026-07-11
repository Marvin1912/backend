package com.marvin.grocery.dto;

import com.marvin.grocery.entity.Supermarket;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing a single historical price data point for a product.
 *
 * @param date        effective purchase date (the receipt's receiptDate, falling back to its creationDate)
 * @param singlePrice price per unit at this purchase
 * @param quantity    number of units purchased
 * @param supermarket supermarket where this purchase was made; null if not set
 * @param receiptId   id of the receipt this purchase belongs to
 */
@Schema(description = "A single historical price data point for a product")
public record PriceHistoryPointDTO(
        @Schema(description = "Effective purchase date (receiptDate, falling back to creationDate)")
        LocalDate date,

        @Schema(description = "Price per unit at this purchase", example = "1.29")
        BigDecimal singlePrice,

        @Schema(description = "Number of units purchased", example = "2")
        int quantity,

        @Schema(description = "Supermarket where this purchase was made; null if not set")
        Supermarket supermarket,

        @Schema(description = "Id of the receipt this purchase belongs to")
        UUID receiptId
) {
}
