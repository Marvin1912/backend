package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request body for excluding a day from nutrition evaluations.
 *
 * @param reason optional free-text reason (max. 255 characters), or null
 */
@Schema(description = "Request to exclude a day from nutrition evaluations")
public record ExcludeDayRequest(
        @Size(max = 255)
        @Schema(description = "Optional reason for the exclusion", example = "Restaurant")
        String reason
) {
}
