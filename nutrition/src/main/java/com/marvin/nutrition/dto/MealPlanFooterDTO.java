package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Data Transfer Object representing the meal plan's closing footer.
 *
 * @param note    closing note explaining where the nutritional data comes from
 * @param sources external sources consulted for data not already present in the tracking database
 */
@Schema(description = "The meal plan's closing footer")
public record MealPlanFooterDTO(
        @Schema(description = "Closing note explaining where the nutritional data comes from")
        String note,

        @Schema(description = "External sources consulted for data not already present in the tracking database")
        List<MealPlanSourceDTO> sources
) {
}
