package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request body for updating the date printed on a receipt.
 *
 * @param receiptDate the purchase date printed on the receipt
 */
@Schema(description = "Receipt date update payload")
public record UpdateReceiptDateRequest(

        @NotNull
        @Schema(description = "Date printed on the receipt", example = "2026-08-21")
        LocalDate receiptDate
) {
}
