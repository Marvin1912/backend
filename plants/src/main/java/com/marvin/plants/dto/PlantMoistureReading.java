package com.marvin.plants.dto;

import java.time.Instant;

/**
 * Represents the most recent soil moisture reading for a single plant, as read from InfluxDB.
 *
 * @param plantName      the name of the plant this reading belongs to
 * @param moisturePercent the measured soil moisture in percent
 * @param measuredAt     the instant at which the measurement was taken
 */
public record PlantMoistureReading(
        String plantName,
        Double moisturePercent,
        Instant measuredAt
) {
}
