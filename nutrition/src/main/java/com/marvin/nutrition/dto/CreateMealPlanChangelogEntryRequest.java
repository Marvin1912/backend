package com.marvin.nutrition.dto;

import com.marvin.nutrition.dto.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for appending a new entry to the meal plan's changelog.
 * The changelog is an append-only historical log, so {@code tag}, {@code text} and {@code sortOrder}
 * are required; only {@code was} is optional since newly introduced items have no previous value.
 *
 * @param tag       short label identifying what changed (required)
 * @param was       the previous value, or {@code null} if this is a newly introduced item
 * @param text      description of the change (required)
 * @param sortOrder display position of the entry (required)
 */
@Schema(description = "Request to append a new entry to the meal plan's changelog")
public record CreateMealPlanChangelogEntryRequest(
        @NotBlank
        @Size(max = 255)
        @Schema(description = "Short label identifying what changed", example = "Whey")
        String tag,

        @NullOrNotBlank
        @Size(max = 500)
        @Schema(description = "Previous value, absent if this is a newly introduced item", example = "80 g/Tag (2×40 g)")
        String was,

        @NotBlank
        @Schema(description = "Description of the change", example = "→ 40 g/Tag")
        String text,

        @NotNull
        @Schema(description = "Display position of the entry", example = "0")
        Integer sortOrder
) {
}
