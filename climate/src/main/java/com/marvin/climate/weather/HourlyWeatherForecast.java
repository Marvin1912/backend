package com.marvin.climate.weather;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Represents a single raw 3-hour interval weather forecast entry from the OpenWeatherMap
 * forecast time series, unaggregated - unlike {@link WeatherForecast}, multiple entries can
 * share the same calendar day.
 *
 * @param dateTime     the date and time this forecast entry represents
 * @param iconCode     the OpenWeatherMap icon code (e.g. {@code "10d"}); the {@code d}/{@code n}
 *                      suffix denotes day/night. Resolving this to an image URL is a frontend concern
 * @param weatherId    the standardized OpenWeatherMap condition id (e.g. {@code 500} for light rain)
 * @param description  the human-readable weather condition description
 * @param temperatureC the forecast temperature in degrees Celsius
 * @param humidityPct  the forecast relative humidity in percent, or {@code null} when unavailable
 * @param windSpeedMs  the forecast wind speed in meters per second, or {@code null} when unavailable
 * @param latitude     the latitude of the location this forecast was requested for
 * @param longitude    the longitude of the location this forecast was requested for
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HourlyWeatherForecast(
        LocalDateTime dateTime,
        String iconCode,
        int weatherId,
        String description,
        double temperatureC,
        Double humidityPct,
        Double windSpeedMs,
        double latitude,
        double longitude
) {
}
