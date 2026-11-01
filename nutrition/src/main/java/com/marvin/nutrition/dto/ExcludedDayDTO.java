package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Data Transfer Object for a day excluded from nutrition evaluations.
 *
 * @param date   the excluded day
 * @param reason optional free-text reason (e.g. "Restaurant"), or null
 */
@Schema(description = "A day excluded from nutrition evaluations")
public record ExcludedDayDTO(
        @Schema(description = "The excluded day", example = "2026-10-20")
        LocalDate date,

        @Schema(description = "Optional reason for the exclusion", example = "Restaurant")
        String reason
) {
}
