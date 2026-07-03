package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing a single item on the shopping list.
 *
 * @param name      the item's name
 * @param brand     brand or sourcing note, or {@code null} if none
 * @param badge     badge severity, either {@code "ok"} or {@code "warn"}, or {@code null} if none
 * @param badgeText text shown alongside the badge, or {@code null} if none
 * @param qty       quantity to buy as a display string
 */
@Schema(description = "A single item on the shopping list")
public record MealPlanShoppingItemDTO(
        @Schema(description = "Item name", example = "Hähnchenbrustfilet")
        String name,

        @Schema(description = "Brand or sourcing note, absent if none", example = "frisch, Kühltheke")
        String brand,

        @Schema(description = "Badge severity, absent if none", example = "warn", allowableValues = {"ok", "warn"})
        String badge,

        @Schema(description = "Text shown alongside the badge, absent if none", example = "nur 1.200 g verfügbar")
        String badgeText,

        @Schema(description = "Quantity to buy as a display string", example = "1.200 g")
        String qty
) {
}
