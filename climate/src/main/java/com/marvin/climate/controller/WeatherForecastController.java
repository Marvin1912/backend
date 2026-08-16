package com.marvin.climate.controller;

import com.marvin.climate.weather.OpenWeatherMapClient;
import com.marvin.climate.weather.WeatherForecast;
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
 * REST controller exposing weather forecast data sourced from OpenWeatherMap. The API key is
 * used server-side only and never reaches the frontend.
 */
@RestController
@RequestMapping("/climate")
@Tag(name = "Weather", description = "Endpoints for retrieving weather forecast data")
public class WeatherForecastController {

    private final OpenWeatherMapClient openWeatherMapClient;

    /**
     * Constructs a WeatherForecastController with the given client.
     *
     * @param openWeatherMapClient the client used to fetch weather forecast data
     */
    public WeatherForecastController(OpenWeatherMapClient openWeatherMapClient) {
        this.openWeatherMapClient = openWeatherMapClient;
    }

    /**
     * Returns the weather forecast for up to the next three days, one representative entry per
     * day (the entry closest to midday), including icon code and weather condition id.
     *
     * @return a Flux of {@link WeatherForecast} objects, potentially empty when no data is available
     */
    @GetMapping("/forecast")
    @Operation(
            summary = "Get weather forecast",
            description = "Retrieves the weather forecast for up to the next three days, including icon code, "
                    + "weather condition id, description, temperature, humidity and wind speed.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Weather forecast retrieved successfully",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = WeatherForecast.class)))
                )
            }
    )
    public Flux<WeatherForecast> getForecast() {
        return openWeatherMapClient.getForecast();
    }
}
