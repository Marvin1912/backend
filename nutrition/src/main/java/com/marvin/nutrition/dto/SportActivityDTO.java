package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.SportActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing a logged sport/exercise activity.
 *
 * @param id           server-assigned unique identifier
 * @param entryDate    the date this activity belongs to
 * @param activityType the activity category (RUNNING, SWIMMING, CYCLING, WALKING, STRENGTH_TRAINING, OTHER)
 * @param description  free-text label, required when {@code activityType} is OTHER
 * @param kcalBurned   kilocalories burned during the activity
 */
@Schema(description = "A logged sport/exercise activity for a given day")
public record SportActivityDTO(
        @Schema(description = "Sport activity identifier")
        UUID id,

        @Schema(description = "Date of the activity", example = "2026-06-07")
        LocalDate entryDate,

        @Schema(description = "Activity category", example = "RUNNING")
        SportActivityType activityType,

        @Schema(description = "Free-text description; required when activityType is OTHER", example = "Climbing")
        String description,

        @Schema(description = "Kilocalories burned", example = "300.00")
        BigDecimal kcalBurned
) {
}
