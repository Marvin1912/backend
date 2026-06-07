package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.ProfileDTO;
import com.marvin.nutrition.service.NutritionProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing the user's nutrition profile.
 * There is at most one profile row; PUT upserts it.
 */
@RestController
@RequestMapping("/nutrition/profile")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class NutritionProfileController {

    private final NutritionProfileService profileService;

    /**
     * Creates a new NutritionProfileController.
     *
     * @param profileService the service handling profile reads and upserts
     */
    public NutritionProfileController(NutritionProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Returns the current nutrition profile, or 404 if none has been created.
     *
     * @return a Mono with 200 and the profile DTO, or 404 if not found
     */
    @GetMapping
    @Operation(
            summary = "Get nutrition profile",
            description = "Returns the current nutrition profile.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Profile returned",
                        content = @Content(schema = @Schema(implementation = ProfileDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "No profile set yet")
            }
    )
    public Mono<ResponseEntity<ProfileDTO>> getProfile() {
        return profileService.getProfile()
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Creates or replaces the nutrition profile.
     *
     * @param dto the profile data to persist
     * @return a Mono with 200 and the saved profile DTO
     */
    @PutMapping
    @Operation(
            summary = "Upsert nutrition profile",
            description = "Creates or replaces the single nutrition profile row.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Profile saved",
                        content = @Content(schema = @Schema(implementation = ProfileDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed")
            }
    )
    public Mono<ResponseEntity<ProfileDTO>> upsertProfile(@Valid @RequestBody ProfileDTO dto) {
        return profileService.upsertProfile(dto)
                .map(ResponseEntity::ok);
    }
}
