package com.marvin.climate.controller;

import com.marvin.climate.dto.WeatherSensitivity;
import com.marvin.climate.service.WeatherSensitivityService;
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
 * REST controller exposing the weather sensitivity assessment.
 */
@RestController
@RequestMapping("/climate")
@Tag(name = "Weather sensitivity", description = "Tendency heuristic for weather sensitivity, not a medical statement")
public class WeatherSensitivityController {

    private final WeatherSensitivityService weatherSensitivityService;

    /**
     * Constructs a WeatherSensitivityController with the given service.
     *
     * @param weatherSensitivityService the service computing the assessment
     */
    public WeatherSensitivityController(WeatherSensitivityService weatherSensitivityService) {
        this.weatherSensitivityService = weatherSensitivityService;
    }

    /**
     * Returns the current weather sensitivity assessment.
     *
     * @return a Mono emitting the {@link WeatherSensitivity}; missing data sources never cause an error
     */
    @GetMapping("/weather-sensitivity")
    @Operation(
            summary = "Get weather sensitivity traffic light",
            description = "Returns level (KEINE, GERING, MITTEL, HOCH), trend, reasons and underlying metrics. "
                    + "A tendency heuristic, not a medical statement.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Assessment computed successfully",
                        content = @Content(schema = @Schema(implementation = WeatherSensitivity.class))
                )
            }
    )
    public Mono<WeatherSensitivity> getWeatherSensitivity() {
        return weatherSensitivityService.getWeatherSensitivity();
    }
}
