package com.marvin.climate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Represents a single climate sensor reading combining temperature and (optionally) humidity.
 *
 * @param sensorId     the unique identifier of the temperature sensor
 * @param label        the human-readable label for the sensor
 * @param location     the physical location of the sensor
 * @param temperatureC the measured temperature in degrees Celsius
 * @param humidityPct  the measured relative humidity in percent, or {@code null} when unavailable
 * @param measuredAt   the instant at which the temperature measurement was taken
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemperatureReading(
        String sensorId,
        String label,
        String location,
        double temperatureC,
        Double humidityPct,
        Instant measuredAt
) {
}
