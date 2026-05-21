package com.marvin.plants.controller;

import com.marvin.image.service.ImageService;
import com.marvin.plants.dto.PlantDTO;
import com.marvin.plants.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

/**
 * REST Controller for managing plants. Provides endpoints for CRUD operations and plant care activities.
 */
@RestController
@RequestMapping(path = "/plants")
@Tag(name = "Plant Management", description = "Endpoints for managing plants, including CRUD operations and care tracking")
public class PlantController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlantController.class);
    private static final String PLANTS_LOCATION_PREFIX = "/plants/";
    private static final String EMPTY_STRING = "";

    private final PlantService plantService;
    private final ImageService imageService;

    public PlantController(PlantService plantService, ImageService imageService) {
        this.plantService = plantService;
        this.imageService = imageService;
    }

    /**
     * Converts a FilePart to a byte array. Handles empty files and errors gracefully by returning empty byte array.
     *
     * @param filePartMono Mono containing the file part to convert
     * @return Mono containing the byte array representation of the file
     */
    private static Mono<byte[]> getFileAsByteArray(Mono<FilePart> filePartMono) {
        return filePartMono
                .flatMap(PlantController::extractBytesFromFilePart)
                .switchIfEmpty(Mono.just(new byte[0]))
                .onErrorResume(PlantController::handleFileReadError);
    }

    /**
     * Extracts bytes from a FilePart by reading its content.
     *
     * @param filePart the file part to extract bytes from
     * @return Mono containing the extracted bytes
     */
    private static Mono<byte[]> extractBytesFromFilePart(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    final byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return Mono.just(bytes);
                });
    }

    /**
     * Handles file read errors by logging and returning empty byte array.
     *
     * @param error the error that occurred
     * @return Mono containing empty byte array
     */
    private static Mono<byte[]> handleFileReadError(Throwable error) {
        LOGGER.error("Error reading file", error);
        return Mono.just(new byte[0]);
    }

    /**
     * Creates a new plant with optional image.
     *
     * @param filePartMono Optional image file
     * @param plantMono    Plant data
     * @param contentType  Content type of the image (optional)
     * @return Mono containing ResponseEntity with location of created plant
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new plant",
            description = "Creates a new plant entry with optional image upload. Returns the location of the created plant in the Location header.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Plant created successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid input data")
            }
    )
    public Mono<ResponseEntity<Object>> createPlant(
            @RequestPart(value = "image", required = false) 
            @Parameter(description = "Optional image file of the plant") Mono<FilePart> filePartMono,
            @RequestPart("plant") 
            @Parameter(description = "Plant data in JSON format") Mono<PlantDTO> plantMono,
            @RequestParam(name = "content-type", required = false) 
            @Parameter(description = "MIME type of the uploaded image") String contentType
    ) {
        return Mono.zip(plantMono, getFileAsByteArray(filePartMono))
                .doOnError(this::logPlantCreationError)
                .flatMap(plantAndImage -> processImageAndCreatePlant(plantAndImage, contentType));
    }

    /**
     * Processes image and creates plant using the provided data.
     *
     * @param plantAndImage Tuple containing PlantDTO and image bytes
     * @param contentType   Content type of the image
     * @return Mono containing ResponseEntity with location of created plant
     */
    private Mono<ResponseEntity<Object>> processImageAndCreatePlant(
            Tuple2<PlantDTO, byte[]> plantAndImage, String contentType) {

        final PlantDTO plantDTO = plantAndImage.getT1();
        final byte[] imageBytes = plantAndImage.getT2();

        return saveImageIfNotEmpty(imageBytes, contentType)
                .flatMap(imageUuid -> createPlantWithImage(plantDTO, imageUuid));
    }

    /**
     * Saves image if bytes are not empty, otherwise returns empty UUID string.
     *
     * @param imageBytes  Image bytes to save
     * @param contentType Content type of the image
     * @return Mono containing UUID string of saved image or empty string
     */
    private Mono<String> saveImageIfNotEmpty(byte[] imageBytes, String contentType) {
        if (imageBytes.length == 0) {
            return Mono.just(EMPTY_STRING);
        }

        return imageService.saveImage(imageBytes, contentType)
                .map(UUID::toString);
    }

    /**
     * Creates plant with the provided image UUID.
     *
     * @param plantDTO  Plant data to create
     * @param imageUuid UUID of the saved image (empty string if no image)
     * @return Mono containing ResponseEntity with location of created plant
     */
    private Mono<ResponseEntity<Object>> createPlantWithImage(PlantDTO plantDTO, String imageUuid) {
        return Mono.fromCallable(() -> {
            final long plantId = plantService.createPlant(plantDTO, imageUuid);
            return createCreatedResponse(plantId);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates a ResponseEntity with CREATED status and Location header.
     *
     * @param plantId ID of the created plant
     * @return ResponseEntity with location header
     */
    private ResponseEntity<Object> createCreatedResponse(long plantId) {
        final URI location = URI.create(PLANTS_LOCATION_PREFIX + plantId);
        return ResponseEntity.created(location).build();
    }

    /**
     * Logs errors that occur during plant creation.
     *
     * @param throwable the error that occurred
     */
    private void logPlantCreationError(Throwable throwable) {
        LOGGER.error("Error creating plant", throwable);
    }

    /**
     * Updates an existing plant.
     *
     * @param plantMono Mono containing updated plant data
     * @return Mono containing ResponseEntity with NO_CONTENT status
     */
    @PutMapping
    @Operation(
            summary = "Update an existing plant",
            description = "Updates the details of an existing plant. The plant ID must be provided within the request body.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Plant updated successfully"),
                @ApiResponse(responseCode = "404", description = "Plant not found")
            }
    )
    public Mono<ResponseEntity<Object>> updatePlant(@RequestBody Mono<PlantDTO> plantMono) {
        return plantMono.flatMap(this::updatePlantSynchronously);
    }

    /**
     * Updates plant in a blocking manner wrapped in a callable.
     *
     * @param plantDTO Plant data to update
     * @return Mono containing ResponseEntity with NO_CONTENT status
     */
    private Mono<ResponseEntity<Object>> updatePlantSynchronously(PlantDTO plantDTO) {
        return Mono.fromCallable(() -> {
            plantService.updatePlant(plantDTO);
            return ResponseEntity.noContent().build();
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieves all plants.
     *
     * @return Flux containing all plants
     */
    @GetMapping
    @Operation(
            summary = "Get all plants",
            description = "Retrieves a list of all plants currently managed in the system.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "List of plants retrieved successfully",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlantDTO.class)))
                )
            }
    )
    public Flux<PlantDTO> getPlants() {
        return plantService.getPlants();
    }

    /**
     * Retrieves a specific plant by ID. Returns empty Mono if plant is not found.
     *
     * @param id ID of the plant to retrieve
     * @return Mono containing the plant data or empty if not found
     */
    @GetMapping(path = "/{id}")
    @Operation(
            summary = "Get plant by ID",
            description = "Retrieves detailed information about a specific plant by its unique identifier.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Plant found and returned",
                        content = @Content(schema = @Schema(implementation = PlantDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Plant not found")
            }
    )
    public Mono<PlantDTO> getPlant(
            @PathVariable @Parameter(description = "ID of the plant to retrieve") long id) {
        return Mono.justOrEmpty(plantService.getPlant(id));
    }

    /**
     * Deletes a plant by ID.
     *
     * @param id ID of the plant to delete
     * @return Mono containing ResponseEntity with NO_CONTENT status
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a plant",
            description = "Removes a plant from the system by its ID.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Plant deleted successfully"),
                @ApiResponse(responseCode = "404", description = "Plant not found")
            }
    )
    public Mono<ResponseEntity<Void>> deletePlant(
            @PathVariable @Parameter(description = "ID of the plant to delete") long id) {
        plantService.deletePlant(id);
        return Mono.just(ResponseEntity.noContent().build());
    }

    /**
     * Records that a plant has been watered and updates watering schedule.
     *
     * @param id          ID of the plant to water
     * @param lastWatered Date when the plant was last watered
     * @return Mono containing ResponseEntity with updated plant data
     */
    @PatchMapping("/{id}/watered")
    @Operation(
            summary = "Record watering",
            description = "Updates the last watered date for a plant and recalculates the next scheduled watering date.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Watering recorded successfully",
                        content = @Content(schema = @Schema(implementation = PlantDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Plant not found")
            }
    )
    public Mono<ResponseEntity<PlantDTO>> waterPlant(
            @PathVariable @Parameter(description = "ID of the plant that was watered") long id,
            @RequestParam("last-watered") @Parameter(description = "Date when the plant was watered", example = "2024-01-24") LocalDate lastWatered
    ) {
        return Mono.just(id)
                .flatMap(plantId -> updateWateringDate(plantId, lastWatered))
                .map(ResponseEntity::ok);
    }

    /**
     * Updates the watering date for a plant. Runs on boundedElastic scheduler to avoid blocking the event loop.
     *
     * @param plantId     ID of the plant to update
     * @param lastWatered Date when the plant was last watered
     * @return Mono containing updated plant data
     */
    private Mono<PlantDTO> updateWateringDate(long plantId, LocalDate lastWatered) {
        return Mono.fromCallable(() -> plantService.waterPlant(plantId, lastWatered))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Records that a plant has been fertilized and updates fertilizing schedule.
     *
     * @param id             ID of the plant to fertilize
     * @param lastFertilized Date when the plant was last fertilized
     * @return Mono containing ResponseEntity with updated plant data
     */
    @PatchMapping("/{id}/fertilized")
    @Operation(
            summary = "Record fertilizing",
            description = "Updates the last fertilized date for a plant and recalculates the next scheduled fertilizing date.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Fertilizing recorded successfully",
                        content = @Content(schema = @Schema(implementation = PlantDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Plant not found")
            }
    )
    public Mono<ResponseEntity<PlantDTO>> fertilizePlant(
            @PathVariable @Parameter(description = "ID of the plant that was fertilized") long id,
            @RequestParam("last-fertilized") @Parameter(description = "Date when the plant was fertilized", example = "2024-01-24") LocalDate lastFertilized
    ) {
        return Mono.just(id)
                .flatMap(plantId -> updateFertilizingDate(plantId, lastFertilized))
                .map(ResponseEntity::ok);
    }

    /**
     * Updates the fertilizing date for a plant. Runs on boundedElastic scheduler to avoid blocking the event loop.
     *
     * @param plantId        ID of the plant to update
     * @param lastFertilized Date when the plant was last fertilized
     * @return Mono containing updated plant data
     */
    private Mono<PlantDTO> updateFertilizingDate(long plantId, LocalDate lastFertilized) {
        return Mono.fromCallable(() -> plantService.fertilizePlant(plantId, lastFertilized))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
