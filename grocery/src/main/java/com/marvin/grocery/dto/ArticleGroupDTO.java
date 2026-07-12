package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing an article group.
 *
 * @param id   database identifier of the group
 * @param name display name of the group
 */
@Schema(description = "A group that articles can be assigned to")
public record ArticleGroupDTO(
        @Schema(description = "Database identifier of the article group", example = "3")
        Long id,

        @Schema(description = "Display name of the article group", example = "Dairy")
        String name
) {
}
