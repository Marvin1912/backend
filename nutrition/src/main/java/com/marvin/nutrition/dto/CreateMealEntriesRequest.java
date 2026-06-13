package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for creating multiple meal log entries for a given day in a single transaction.
 *
 * @param entries non-empty list (max 50) of meal entries to create; each element is validated individually
 */
@Schema(description = "Request to log multiple meal entries for a given day in one transaction")
public record CreateMealEntriesRequest(
        @NotEmpty
        @Size(max = 50)
        @Valid
        @Schema(description = "Non-empty list of meal entries to create (max 50)")
        List<CreateMealEntryRequest> entries
) {
}
