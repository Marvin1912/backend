package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.MealEstimateDTO;
import com.marvin.nutrition.dto.MealEstimateRequest;
import com.marvin.nutrition.service.MealEstimator;
import com.marvin.nutrition.service.PhotoMealEstimator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller that estimates macros for a described or photographed canteen meal via Claude.
 * The result is transient — nothing is persisted by this endpoint.
 */
@RestController
@RequestMapping("/nutrition/estimate")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class MealEstimateController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MealEstimateController.class);
    private static final int MAX_MEAL_PHOTO_SIZE_BYTES = 10 * 1024 * 1024;

    private final MealEstimator mealEstimator;
    private final PhotoMealEstimator photoMealEstimator;

    /**
     * Creates a new MealEstimateController.
     *
     * @param mealEstimator      the service that estimates meal macros from a text description via Claude
     * @param photoMealEstimator the service that estimates meal macros from a photo via Claude Vision
     */
    public MealEstimateController(MealEstimator mealEstimator, PhotoMealEstimator photoMealEstimator) {
        this.mealEstimator = mealEstimator;
        this.photoMealEstimator = photoMealEstimator;
    }

    /**
     * Accepts a meal description, calls Claude to estimate macros, and returns the estimate.
     * Nothing is persisted — the returned estimate is only held in memory.
     *
     * @param request the meal description and optional portion hint
     * @return a Mono with 200 OK and the parsed macro estimate DTO
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Estimate macros for a described meal",
            description = "Sends the meal description to Claude and returns a transient macro estimate. "
                    + "Provide an optional portion hint to improve accuracy. The estimate is never persisted.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Macro estimate successfully returned",
                        content = @Content(schema = @Schema(implementation = MealEstimateDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed — description is blank or too long"),
                @ApiResponse(responseCode = "422", description = "Estimation failed — Claude could not produce a usable result")
            }
    )
    public Mono<ResponseEntity<MealEstimateDTO>> estimate(@Valid @RequestBody MealEstimateRequest request) {
        return mealEstimator.estimate(request.description(), request.portionHint())
                .map(ResponseEntity::ok);
    }

    /**
     * Accepts a meal photo, calls Claude Vision to estimate macros, and returns the estimate.
     * Nothing is persisted — the returned estimate is only held in memory.
     *
     * @param filePart    the multipart file containing the meal photo (JPEG or PNG)
     * @param portionHint optional hint about the portion size; may be {@code null}
     * @return a Mono with 200 OK and the parsed macro estimate DTO
     */
    @PostMapping(path = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Estimate macros for a photographed meal",
            description = "Sends the uploaded meal photo to Claude Vision and returns a transient macro estimate. "
                    + "Provide an optional portion hint to improve accuracy. The estimate is never persisted.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Macro estimate successfully returned",
                        content = @Content(schema = @Schema(implementation = MealEstimateDTO.class))
                ),
                @ApiResponse(responseCode = "413", description = "Uploaded file exceeds the maximum allowed size"),
                @ApiResponse(responseCode = "422", description = "Estimation failed — Claude could not produce a usable result")
            }
    )
    public Mono<ResponseEntity<MealEstimateDTO>> estimateFromPhoto(
            @RequestPart("file")
            @Parameter(description = "Meal photo file (JPEG or PNG)") FilePart filePart,
            @RequestPart(value = "portionHint", required = false)
            @Parameter(description = "Optional hint about the portion size") String portionHint) {
        LOGGER.info("Received estimate/photo request: filename={}", filePart.filename());
        return extractBytes(filePart)
                .flatMap(bytes -> photoMealEstimator.estimateFromPhoto(bytes, portionHint))
                .map(ResponseEntity::ok);
    }

    /**
     * Reads all bytes from the given FilePart reactively.
     *
     * @param filePart the file part to read
     * @return a Mono emitting the complete file bytes
     */
    private Mono<byte[]> extractBytes(FilePart filePart) {
        return DataBufferUtils.join(filePart.content(), MAX_MEAL_PHOTO_SIZE_BYTES)
                .map(dataBuffer -> {
                    final byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .doOnError(e -> LOGGER.error("Failed to read uploaded meal photo", e));
    }
}
