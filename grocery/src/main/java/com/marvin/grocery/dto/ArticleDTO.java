package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing a normalized grocery article and its group assignment.
 *
 * @param normalizedName the normalized (lower-cased, trimmed) name used for de-duplication
 * @param name           the display name of the article
 * @param groupId        the id of the assigned article group, or null if unassigned
 * @param groupName      the name of the assigned article group, or null if unassigned
 * @param purchaseCount  the number of receipt items referencing this article
 */
@Schema(description = "A normalized grocery article with its optional group assignment and purchase count")
public record ArticleDTO(
        @Schema(description = "Normalized (lower-cased, trimmed) article name", example = "vollmilch 3,5%")
        String normalizedName,

        @Schema(description = "Display name of the article", example = "Vollmilch 3,5%")
        String name,

        @Schema(description = "Id of the assigned article group, null if unassigned", example = "3")
        Long groupId,

        @Schema(description = "Name of the assigned article group, null if unassigned", example = "Dairy")
        String groupName,

        @Schema(description = "Number of receipt items referencing this article", example = "12")
        long purchaseCount
) {
}
