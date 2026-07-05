package com.marvin.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body returned with HTTP 409 when a food catalog entry cannot be deleted because it is
 * still referenced by other data.
 *
 * @param referencedBy breakdown of the entities referencing the food
 */
@Schema(description = "Conflict response body for a food deletion blocked by existing references")
public record FoodReferencedResponse(
        @Schema(description = "Breakdown of the entities referencing the food")
        FoodReferencedByDTO referencedBy
) {
}
