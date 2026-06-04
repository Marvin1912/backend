package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Request body for manually adding a new item to a receipt.
 *
 * @param name        name of the item
 * @param quantity    number of units
 * @param singlePrice price per unit in euros
 */
@Schema(description = "Fields for a new receipt item")
public record AddReceiptItemRequest(

        @Schema(description = "Name of the purchased item", example = "Vollmilch 3,5%")
        String name,

        @Schema(description = "Number of units purchased", example = "2")
        int quantity,

        @Schema(description = "Price per unit in euros", example = "1.29")
        BigDecimal singlePrice
) {
}
