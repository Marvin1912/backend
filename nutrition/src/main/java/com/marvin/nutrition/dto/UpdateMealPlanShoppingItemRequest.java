package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating a meal-plan shopping-list item.
 * All fields are optional; only non-null values are applied.
 *
 * @param name      updated item name (nullable)
 * @param brand     updated brand or sourcing note (nullable)
 * @param badge     updated badge severity, either {@code "ok"} or {@code "warn"} (nullable)
 * @param badgeText updated text shown alongside the badge (nullable)
 * @param qty       updated quantity to buy as a display string (nullable)
 */
@Schema(description = "Request to update a meal-plan shopping-list item")
public record UpdateMealPlanShoppingItemRequest(
        @NullOrNotBlank
        @Size(max = 255)
        @Schema(description = "Updated item name", example = "Hähnchenbrustfilet")
        String name,

        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Updated brand or sourcing note", example = "frisch, Kühltheke")
        String brand,

        @NullOrNotBlank
        @Pattern(regexp = "^(ok|warn)$")
        @Schema(description = "Updated badge severity", example = "warn", allowableValues = {"ok", "warn"})
        String badge,

        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Updated text shown alongside the badge", example = "nur 1.200 g verfügbar")
        String badgeText,

        @NullOrNotBlank
        @Size(max = 255)
        @Schema(description = "Updated quantity to buy as a display string", example = "1.200 g")
        String qty
) {
}
