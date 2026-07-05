package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for creating multiple meal-plan rows within a section in a single transaction.
 *
 * @param rows non-empty list (max 50) of rows to create; each element is validated individually
 */
@Schema(description = "Request to create multiple meal-plan rows within a section in one transaction")
public record CreateMealPlanRowsRequest(
        @NotEmpty
        @Size(max = 50)
        @Valid
        @Schema(description = "Non-empty list of rows to create (max 50)")
        List<CreateMealPlanRowRequest> rows
) {
}
