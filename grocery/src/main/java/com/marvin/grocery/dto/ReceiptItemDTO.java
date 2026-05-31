package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing a single line item on a grocery receipt.
 *
 * @param id          database identifier of the item
 * @param name        name of the purchased item
 * @param singlePrice price per unit in euros
 * @param quantity    number of units purchased
 * @param price       total line price in euros (singlePrice × quantity)
 */
@Schema(description = "A single parsed item from a grocery receipt")
public record ReceiptItemDTO(
        @Schema(description = "Database identifier of the item", example = "42")
        Long id,

        @Schema(description = "Name of the purchased item", example = "Vollmilch 3,5%")
        String name,

        @Schema(description = "Price per unit of the item in euros", example = "1.29")
        BigDecimal singlePrice,

        @Schema(description = "Number of units purchased", example = "2")
        int quantity,

        @Schema(description = "Total line price in euros", example = "2.58")
        BigDecimal price
) {
}
