package com.marvin.climate.biometeorology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BiometeorologyCalculator#relativeHumidity(double, double)}.
 */
class BiometeorologyCalculatorRelativeHumidityTest {

    @Test
    void relativeHumidity_isInverseOfDewPoint() {
        final double dewPoint = BiometeorologyCalculator.dewPoint(20.0, 50.0);
        assertEquals(50.0, BiometeorologyCalculator.relativeHumidity(20.0, dewPoint), 1e-6);
    }

    @Test
    void relativeHumidity_withDewPointEqualToTemperature_is100() {
        assertEquals(100.0, BiometeorologyCalculator.relativeHumidity(15.0, 15.0), 1e-9);
    }

    @Test
    void relativeHumidity_withDewPointAboveTemperature_isCappedAt100() {
        assertEquals(100.0, BiometeorologyCalculator.relativeHumidity(15.0, 16.0), 1e-9);
    }

    @Test
    void relativeHumidity_withNaN_throws() {
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.relativeHumidity(Double.NaN, 10.0));
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.relativeHumidity(10.0, Double.NaN));
    }
}
