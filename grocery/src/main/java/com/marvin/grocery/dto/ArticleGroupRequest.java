package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or renaming an article group.
 *
 * @param name the new display name of the group
 */
@Schema(description = "Fields for creating or renaming an article group")
public record ArticleGroupRequest(

        @Schema(description = "Display name of the article group", example = "Dairy")
        @NotBlank
        @Size(max = 255)
        String name
) {
}
