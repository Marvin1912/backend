package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing a single line item on a grocery receipt.
 *
 * @param id    database identifier of the item
 * @param name  name of the purchased item
 * @param price price of the item in euros
 */
@Schema(description = "A single parsed item from a grocery receipt")
public record ReceiptItemDTO(
        @Schema(description = "Database identifier of the item", example = "42")
        Long id,

        @Schema(description = "Name of the purchased item", example = "Vollmilch 3,5%")
        String name,

        @Schema(description = "Price of the item in euros", example = "1.29")
        BigDecimal price
) {
}
