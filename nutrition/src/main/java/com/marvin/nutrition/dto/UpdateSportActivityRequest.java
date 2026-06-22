package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import com.marvin.nutrition.entity.SportActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body for updating an existing sport/exercise activity.
 * All fields are optional; only non-null values are applied.
 * If the resulting activity type is OTHER, a non-blank description is required; this is
 * enforced imperatively by the write service rather than a bean validation constraint.
 *
 * @param activityType updated activity category (nullable)
 * @param description  updated free-text label (nullable)
 * @param kcalBurned   updated kilocalories burned (nullable)
 */
@Schema(description = "Request to update an existing sport/exercise activity")
public record UpdateSportActivityRequest(
        @Schema(description = "Updated activity category (nullable)", example = "CYCLING")
        SportActivityType activityType,

        @Size(max = 255)
        @NullOrNotBlank
        @Schema(description = "Updated free-text description", example = "Climbing")
        String description,

        @PositiveOrZero
        @Schema(description = "Updated kilocalories burned", example = "400.00")
        BigDecimal kcalBurned
) {
}
