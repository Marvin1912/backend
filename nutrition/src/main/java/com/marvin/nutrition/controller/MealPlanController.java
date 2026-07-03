package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.service.MealPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller serving the static weekly meal-plan reference document.
 * This is read-only content bundled with the application, not user-editable data.
 */
@RestController
@RequestMapping("/nutrition/meal-plan")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class MealPlanController {

    private final MealPlanService mealPlanService;

    /**
     * Creates a new MealPlanController.
     *
     * @param mealPlanService the service serving the bundled meal-plan document
     */
    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    /**
     * Returns the static weekly meal-plan reference document.
     *
     * @return a Mono emitting the meal-plan DTO
     */
    @GetMapping
    @Operation(
            summary = "Get the weekly meal plan",
            description = "Returns the static weekly meal-plan reference document, including the shopping list.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Meal plan returned",
                        content = @Content(schema = @Schema(implementation = MealPlanDTO.class))
                )
            }
    )
    public Mono<MealPlanDTO> getMealPlan() {
        return mealPlanService.getMealPlan();
    }
}
