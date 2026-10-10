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
 * @param pressure     the atmospheric pressure in hPa, or {@code null} when unavailable
 * @param feelsLike    the perceived ("feels like") temperature in degrees Celsius, or {@code null} when unavailable
 * @param dewPointC    the dew point in degrees Celsius, or {@code null} when unavailable
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
        Double pressure,
        Double feelsLike,
        Double dewPointC,
        double latitude,
        double longitude
) {

    /**
     * Creates a forecast without dew point ({@code null}).
     *
     * @param dateTime     the date and time this forecast entry represents
     * @param iconCode     the icon code
     * @param weatherId    the condition id
     * @param description  the weather condition description
     * @param temperatureC the temperature in degrees Celsius
     * @param humidityPct  the relative humidity in percent, or {@code null}
     * @param windSpeedMs  the wind speed in meters per second, or {@code null}
     * @param pressure     the atmospheric pressure in hPa, or {@code null}
     * @param feelsLike    the perceived temperature in degrees Celsius, or {@code null}
     * @param latitude     the latitude of the forecast location
     * @param longitude    the longitude of the forecast location
     */
    public HourlyWeatherForecast(LocalDateTime dateTime, String iconCode, int weatherId, String description,
            double temperatureC, Double humidityPct, Double windSpeedMs, Double pressure, Double feelsLike,
            double latitude, double longitude) {
        this(dateTime, iconCode, weatherId, description, temperatureC, humidityPct, windSpeedMs, pressure, feelsLike, null,
                latitude, longitude);
    }

    /**
     * Creates a forecast without pressure and feels-like temperature (both {@code null}).
     *
     * @param dateTime     the date and time this forecast entry represents
     * @param iconCode     the OpenWeatherMap icon code
     * @param weatherId    the OpenWeatherMap condition id
     * @param description  the weather condition description
     * @param temperatureC the temperature in degrees Celsius
     * @param humidityPct  the relative humidity in percent, or {@code null}
     * @param windSpeedMs  the wind speed in meters per second, or {@code null}
     * @param latitude     the latitude of the forecast location
     * @param longitude    the longitude of the forecast location
     */
    public HourlyWeatherForecast(LocalDateTime dateTime, String iconCode, int weatherId, String description,
            double temperatureC, Double humidityPct, Double windSpeedMs, double latitude, double longitude) {
        this(dateTime, iconCode, weatherId, description, temperatureC, humidityPct, windSpeedMs, null, null, null, latitude, longitude);
    }
}
