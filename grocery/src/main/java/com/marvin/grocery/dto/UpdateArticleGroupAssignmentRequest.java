package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for assigning or clearing an article's group.
 *
 * @param groupId the id of the group to assign, or null to clear the assignment
 */
@Schema(description = "Article group assignment payload")
public record UpdateArticleGroupAssignmentRequest(

        @Schema(description = "Id of the article group to assign; null clears the assignment", example = "3")
        Long groupId
) {
}
