package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Request body for updating a receipt item's editable fields.
 *
 * @param name        updated name of the item
 * @param quantity    updated quantity
 * @param singlePrice updated price per unit in euros
 */
@Schema(description = "Fields to update on a receipt item")
public record UpdateReceiptItemRequest(

        @Schema(description = "Name of the purchased item", example = "Vollmilch 3,5%")
        String name,

        @Schema(description = "Number of units purchased", example = "2")
        int quantity,

        @Schema(description = "Price per unit in euros", example = "1.29")
        BigDecimal singlePrice
) {
}
