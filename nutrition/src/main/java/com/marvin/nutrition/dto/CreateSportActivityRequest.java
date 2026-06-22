package com.marvin.nutrition.dto;

import com.marvin.nutrition.entity.SportActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body for logging a new sport/exercise activity.
 * When {@code activityType} is OTHER, a non-blank {@code description} is required;
 * this is enforced imperatively by the write service rather than a bean validation constraint.
 *
 * @param activityType the activity category (required)
 * @param description  free-text label (required when {@code activityType} is OTHER)
 * @param kcalBurned   kilocalories burned during the activity (required)
 */
@Schema(description = "Request to log a sport/exercise activity for a given day")
public record CreateSportActivityRequest(
        @NotNull
        @Schema(description = "Activity category", example = "RUNNING")
        SportActivityType activityType,

        @Size(max = 255)
        @Schema(description = "Free-text description; required when activityType is OTHER", example = "Climbing")
        String description,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Kilocalories burned", example = "300.00")
        BigDecimal kcalBurned
) {
}
