package com.marvin.climate.controller;

import com.marvin.climate.dto.TemperatureReading;
import com.marvin.climate.service.ClimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * REST controller exposing climate sensor data endpoints.
 */
@RestController
@RequestMapping("/climate")
@Tag(name = "Climate", description = "Endpoints for retrieving climate sensor readings")
public class ClimateController {

    private final ClimateService climateService;

    /**
     * Constructs a ClimateController with the given service.
     *
     * @param climateService the service used to fetch climate readings
     */
    public ClimateController(ClimateService climateService) {
        this.climateService = climateService;
    }

    /**
     * Returns the most recent temperature reading for each configured climate sensor.
     *
     * @return a Flux of {@link TemperatureReading} objects, potentially empty when no data is available
     */
    @GetMapping("/readings")
    @Operation(
            summary = "Get current climate readings",
            description = "Retrieves the latest temperature reading from each configured climate sensor.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Climate readings retrieved successfully",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = TemperatureReading.class)))
                )
            }
    )
    public Flux<TemperatureReading> getReadings() {
        return climateService.getCurrentReadings();
    }
}
