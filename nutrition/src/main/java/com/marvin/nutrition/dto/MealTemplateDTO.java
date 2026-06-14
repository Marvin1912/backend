package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a reusable meal template, a named collection of food items and quantities.
 *
 * @param id    server-assigned unique identifier
 * @param name  the template's display name
 * @param items the food items and quantities making up this template
 */
@Schema(description = "A reusable meal template, a named collection of food items and quantities")
public record MealTemplateDTO(
        @Schema(description = "Meal template identifier")
        UUID id,

        @Schema(description = "Display name of the template", example = "Breakfast Bowl")
        String name,

        @Schema(description = "Food items and quantities making up this template")
        List<MealTemplateItemDTO> items
) {
}
