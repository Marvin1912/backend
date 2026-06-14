package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for creating a new meal template.
 * An empty {@code items} list is allowed, creating a template with no items yet.
 *
 * @param name  the display name of the template (required, max 255 characters)
 * @param items the food items and quantities making up the template (required, may be empty)
 */
@Schema(description = "Request to create a new meal template")
public record CreateMealTemplateRequest(
        @NotBlank
        @Size(max = 255)
        @Schema(description = "Display name of the template", example = "Breakfast Bowl")
        String name,

        @NotNull
        @Valid
        @Schema(description = "Food items and quantities making up the template")
        List<MealTemplateItemRequest> items
) {
}
