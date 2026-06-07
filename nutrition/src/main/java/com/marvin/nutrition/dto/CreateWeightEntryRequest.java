package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating or replacing a body-weight entry.
 *
 * @param entryDate date the weight was measured
 * @param weightKg  body weight in kilograms, must be positive
 */
@Schema(description = "Request to record a body-weight measurement")
public record CreateWeightEntryRequest(
        @NotNull
        @Schema(description = "Date the weight was measured", example = "2024-06-01")
        LocalDate entryDate,

        @NotNull
        @Positive
        @Schema(description = "Body weight in kilograms", example = "80.5")
        BigDecimal weightKg
) {
}
