package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for a body-weight measurement.
 *
 * @param id          surrogate identifier
 * @param entryDate   date the weight was recorded
 * @param weightKg    body weight in kilograms
 */
@Schema(description = "A single body-weight measurement")
public record WeightEntryDTO(
        @Schema(description = "Weight entry identifier")
        Long id,

        @Schema(description = "Date the weight was recorded", example = "2024-06-01")
        LocalDate entryDate,

        @Schema(description = "Body weight in kilograms", example = "80.5")
        BigDecimal weightKg
) {
}
