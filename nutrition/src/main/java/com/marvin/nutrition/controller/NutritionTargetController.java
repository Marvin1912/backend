package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.service.NutritionTargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for computing and returning daily nutrition targets.
 * Returns 400 with an explanatory message when profile or weight data is missing.
 */
@RestController
@RequestMapping("/nutrition/targets")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class NutritionTargetController {

    private final NutritionTargetService nutritionTargetService;

    /**
     * Creates a new NutritionTargetController.
     *
     * @param nutritionTargetService the service that computes targets
     */
    public NutritionTargetController(NutritionTargetService nutritionTargetService) {
        this.nutritionTargetService = nutritionTargetService;
    }

    /**
     * Returns the computed daily nutrition targets based on the current profile and latest weight.
     * Returns 400 with an explanatory message if the profile or a weight entry is missing.
     *
     * @return a Mono with 200 and the targets DTO
     */
    @GetMapping
    @Operation(
            summary = "Get daily nutrition targets",
            description = "Computes and returns daily kcal and macro targets from the current profile and latest weight.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Targets computed",
                        content = @Content(schema = @Schema(implementation = TargetsDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Profile or weight data is missing")
            }
    )
    public Mono<ResponseEntity<TargetsDTO>> getTargets() {
        return nutritionTargetService.getTargets()
                .map(ResponseEntity::ok);
    }
}
