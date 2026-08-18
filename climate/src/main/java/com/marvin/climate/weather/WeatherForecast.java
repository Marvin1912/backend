package com.marvin.climate.weather;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Represents an aggregated weather forecast for a single day, derived from the
 * OpenWeatherMap 3-hour interval forecast time series by picking the entry closest to midday.
 *
 * @param date         the calendar date this forecast entry represents
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
public record WeatherForecast(
        LocalDate date,
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
