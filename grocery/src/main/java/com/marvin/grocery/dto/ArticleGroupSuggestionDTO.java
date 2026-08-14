package com.marvin.grocery.dto;

import com.marvin.grocery.entity.SuggestionSource;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing a pending suggestion to assign an article to a group.
 *
 * @param id                     database identifier of the suggestion
 * @param articleId              id of the suggested article
 * @param articleName            display name of the suggested article
 * @param articleNormalizedName  normalized name of the suggested article
 * @param suggestedGroupId       id of the suggested group
 * @param suggestedGroupName     name of the suggested group
 * @param score                  the similarity/confidence score that produced this suggestion
 * @param source                 which matching mechanism produced this suggestion
 */
@Schema(description = "A pending suggestion to assign an article to a group")
public record ArticleGroupSuggestionDTO(
        @Schema(description = "Database identifier of the suggestion", example = "1")
        Long id,

        @Schema(description = "Id of the suggested article", example = "5")
        Long articleId,

        @Schema(description = "Display name of the suggested article", example = "Vollmilch 3,5%")
        String articleName,

        @Schema(description = "Normalized name of the suggested article", example = "vollmilch 3,5%")
        String articleNormalizedName,

        @Schema(description = "Id of the suggested article group", example = "3")
        Long suggestedGroupId,

        @Schema(description = "Name of the suggested article group", example = "Dairy")
        String suggestedGroupName,

        @Schema(description = "Similarity/confidence score that produced this suggestion", example = "0.83")
        double score,

        @Schema(description = "Which matching mechanism produced this suggestion", example = "HEURISTIC")
        SuggestionSource source
) {
}
