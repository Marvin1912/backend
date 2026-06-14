package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for updating an existing meal template.
 *
 * <p>This is a full replacement: the template is renamed to {@code name} and its entire item
 * composition is replaced with {@code items} in a single call — existing items not present in
 * {@code items} are removed, and all entries in {@code items} are (re-)created. An empty
 * {@code items} list removes all items from the template.</p>
 *
 * @param name  the new display name of the template (required, max 255 characters)
 * @param items the complete replacement set of food items and quantities (required, may be empty)
 */
@Schema(description = "Request to fully replace an existing meal template's name and item composition")
public record UpdateMealTemplateRequest(
        @NotBlank
        @Size(max = 255)
        @Schema(description = "New display name of the template", example = "Breakfast Bowl")
        String name,

        @NotNull
        @Valid
        @Schema(description = "Complete replacement set of food items and quantities")
        List<MealTemplateItemRequest> items
) {
}
