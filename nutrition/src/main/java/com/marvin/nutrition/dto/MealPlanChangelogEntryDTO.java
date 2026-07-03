package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Data Transfer Object representing a single changelog entry describing a change compared to the previous
 * plan version.
 *
 * @param id   the changelog entry's unique identifier
 * @param tag  short label identifying what changed
 * @param was  the previous value, or {@code null} if this is a newly introduced item
 * @param text description of the change
 */
@Schema(description = "A changelog entry describing a change compared to the previous plan version")
public record MealPlanChangelogEntryDTO(
        @Schema(description = "Changelog entry unique identifier")
        UUID id,

        @Schema(description = "Short label identifying what changed", example = "Whey")
        String tag,

        @Schema(description = "Previous value, absent if this is a newly introduced item", example = "80 g/Tag (2×40 g)")
        String was,

        @Schema(description = "Description of the change")
        String text
) {
}
