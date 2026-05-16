package com.marvin.climate.dto;

import java.time.Instant;

/**
 * Represents a single temperature reading from a climate sensor.
 *
 * @param sensorId     the unique identifier of the sensor
 * @param label        the human-readable label for the sensor
 * @param location     the physical location of the sensor
 * @param temperatureC the measured temperature in degrees Celsius
 * @param measuredAt   the instant at which the measurement was taken
 */
public record TemperatureReading(
        String sensorId,
        String label,
        String location,
        double temperatureC,
        Instant measuredAt
) {
}
