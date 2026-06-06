package com.marvin.grocery.dto;

import com.marvin.grocery.entity.Supermarket;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a scanned grocery receipt.
 *
 * @param id           unique identifier of the receipt
 * @param receiptDate  date printed on the receipt
 * @param totalAmount  total amount computed as sum of all items
 * @param creationDate timestamp when the receipt was created in the system
 * @param items        parsed line items; null when listing receipts without items
 * @param supermarket  supermarket where the receipt was issued; null if not yet set
 */
@Schema(description = "A scanned and parsed grocery receipt")
public record ReceiptDTO(
        @Schema(description = "Unique identifier of the receipt")
        UUID id,

        @Schema(description = "Date printed on the receipt")
        LocalDate receiptDate,

        @Schema(description = "Total amount computed as sum of all items", example = "24.97")
        BigDecimal totalAmount,

        @Schema(description = "Timestamp when the receipt was created in the system")
        LocalDateTime creationDate,

        @Schema(description = "Parsed line items; null when listing receipts without items")
        List<ReceiptItemDTO> items,

        @Schema(description = "Supermarket where the receipt was issued; null if not yet set")
        Supermarket supermarket
) {
}
