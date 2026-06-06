package com.marvin.grocery.dto;

import com.marvin.grocery.entity.Supermarket;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for updating the supermarket on a receipt.
 *
 * @param supermarket the supermarket where the receipt was issued
 */
@Schema(description = "Supermarket update payload")
public record UpdateSupermarketRequest(

        @Schema(description = "Supermarket enum value", example = "LIDL")
        Supermarket supermarket
) {
}
