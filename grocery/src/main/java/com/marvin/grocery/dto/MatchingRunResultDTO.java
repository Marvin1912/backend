package com.marvin.grocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object summarizing the outcome of an article-to-group matching run.
 *
 * @param candidatesEvaluated the number of ungrouped articles considered
 * @param autoAssigned        the number of articles that were assigned to a group automatically
 * @param suggested           the number of articles for which a pending suggestion was queued
 * @param unmatched           the number of articles for which no group could be determined
 */
@Schema(description = "Summary of the outcome of an article-to-group matching run")
public record MatchingRunResultDTO(
        @Schema(description = "Number of ungrouped articles considered", example = "12")
        int candidatesEvaluated,

        @Schema(description = "Number of articles assigned to a group automatically", example = "4")
        int autoAssigned,

        @Schema(description = "Number of articles for which a pending suggestion was queued", example = "3")
        int suggested,

        @Schema(description = "Number of articles for which no group could be determined", example = "5")
        int unmatched
) {
}
