package com.marvin.nutrition.controller;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.dto.FoodDraftDTO;
import com.marvin.nutrition.service.FoodService;
import com.marvin.nutrition.service.LabelReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing the nutrition food catalog.
 * Provides endpoints to create, read, update, delete, search food entries,
 * and scan a nutrition label photo to produce a transient draft food.
 */
@RestController
@RequestMapping("/nutrition/foods")
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class FoodController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FoodController.class);
    private static final String FOODS_LOCATION_PREFIX = "/nutrition/foods/";

    private final FoodService foodService;
    private final LabelReader labelReader;

    /**
     * Creates a new FoodController with the required services.
     *
     * @param foodService the service handling food catalog operations
     * @param labelReader the service that reads nutrition labels via Claude
     */
    public FoodController(FoodService foodService, LabelReader labelReader) {
        this.foodService = foodService;
        this.labelReader = labelReader;
    }

    /**
     * Lists all food entries, optionally filtered by a name search query.
     *
     * @param q optional case-insensitive name contains filter
     * @return a Flux emitting matching food DTOs ordered by name
     */
    @GetMapping
    @Operation(
            summary = "List food entries",
            description = "Returns all food catalog entries, or those whose name contains the given query string (case-insensitive).",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Food list returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = FoodDTO.class)))
                )
            }
    )
    public Flux<FoodDTO> listFoods(
            @RequestParam(required = false)
            @Parameter(description = "Optional name search query (case-insensitive contains)") String q) {
        return foodService.findAll(q);
    }

    /**
     * Returns the food entry with the given id, or 404 if not found.
     *
     * @param id the UUID of the food entry
     * @return a Mono with 200 and the food DTO, or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a food entry by id",
            description = "Returns a single food catalog entry by its UUID.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Food entry returned",
                        content = @Content(schema = @Schema(implementation = FoodDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Food entry not found")
            }
    )
    public Mono<ResponseEntity<FoodDTO>> getFoodById(
            @PathVariable @Parameter(description = "UUID of the food entry") UUID id) {
        return foodService.findById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Creates a new food entry and returns 201 Created with a Location header.
     *
     * @param dto the food data to create
     * @return a Mono with 201 Created, Location header, and the created food DTO
     */
    @PostMapping
    @Operation(
            summary = "Create a food entry",
            description = "Adds a new entry to the food catalog. Source defaults to MANUAL if not specified.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Food entry created; Location header contains the URI",
                        content = @Content(schema = @Schema(implementation = FoodDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed")
            }
    )
    public Mono<ResponseEntity<FoodDTO>> createFood(@Valid @RequestBody FoodDTO dto) {
        return foodService.create(dto)
                .map(created -> {
                    final URI location = URI.create(FOODS_LOCATION_PREFIX + created.id());
                    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
                });
    }

    /**
     * Updates an existing food entry, or returns 404 if not found.
     *
     * @param id  the UUID of the food entry to update
     * @param dto the updated food data
     * @return a Mono with 200 and the updated food DTO, or 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a food entry",
            description = "Replaces an existing food catalog entry with the provided data.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Food entry updated",
                        content = @Content(schema = @Schema(implementation = FoodDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Validation failed"),
                @ApiResponse(responseCode = "404", description = "Food entry not found")
            }
    )
    public Mono<ResponseEntity<FoodDTO>> updateFood(
            @PathVariable @Parameter(description = "UUID of the food entry to update") UUID id,
            @Valid @RequestBody FoodDTO dto) {
        return foodService.update(id, dto)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes the food entry with the given id, or returns 404 if not found.
     *
     * @param id the UUID of the food entry to delete
     * @return a Mono with 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a food entry",
            description = "Permanently removes the food entry from the catalog.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Food entry deleted"),
                @ApiResponse(responseCode = "404", description = "Food entry not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteFood(
            @PathVariable @Parameter(description = "UUID of the food entry to delete") UUID id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        return foodService.delete(id)
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound);
    }

    /**
     * Accepts a food packaging photo, sends it to Claude, and returns a transient draft food.
     * Nothing is persisted — the returned draft is only held in memory.
     *
     * @param filePart the multipart file containing the food label image (JPEG or PNG)
     * @return a Mono with 200 OK and the parsed draft food DTO
     */
    @PostMapping(path = "/scan-label", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Scan a nutrition label photo",
            description = "Sends the uploaded label image to Claude and returns a transient draft food with parsed values. "
                    + "All macros are normalised to per 100 g. The draft is never persisted.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Label successfully read; draft food returned",
                        content = @Content(schema = @Schema(implementation = FoodDraftDTO.class))
                ),
                @ApiResponse(responseCode = "422", description = "Label could not be read or parsed")
            }
    )
    public Mono<ResponseEntity<FoodDraftDTO>> scanLabel(
            @RequestPart("file")
            @Parameter(description = "Nutrition label image file (JPEG or PNG)") FilePart filePart) {
        LOGGER.info("Received scan-label request: filename={}", filePart.filename());
        return extractBytes(filePart)
                .flatMap(labelReader::readLabel)
                .map(draft -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(draft));
    }

    /**
     * Reads all bytes from the given FilePart reactively.
     *
     * @param filePart the file part to read
     * @return a Mono emitting the complete file bytes
     */
    private Mono<byte[]> extractBytes(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    final byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .doOnError(e -> LOGGER.error("Failed to read uploaded label image", e));
    }
}
