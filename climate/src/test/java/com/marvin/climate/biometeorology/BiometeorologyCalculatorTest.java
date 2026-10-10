package com.marvin.climate.biometeorology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BiometeorologyCalculator}.
 */
class BiometeorologyCalculatorTest {

    private static final double DELTA = 0.1;

    @Test
    void dewPoint_at20DegreesAnd50Percent_isAbout9Point3() {
        assertEquals(9.3, BiometeorologyCalculator.dewPoint(20.0, 50.0), DELTA);
    }

    @Test
    void dewPoint_at100PercentHumidity_equalsTemperature() {
        assertEquals(20.0, BiometeorologyCalculator.dewPoint(20.0, 100.0), 1e-9);
        assertEquals(-5.0, BiometeorologyCalculator.dewPoint(-5.0, 100.0), 1e-9);
    }

    @Test
    void dewPoint_atNegativeTemperature_isBelowTemperature() {
        final double td = BiometeorologyCalculator.dewPoint(-10.0, 80.0);
        assertEquals(-12.8, td, DELTA);
        assertTrue(td < -10.0);
    }

    @Test
    void dewPoint_withZeroOrNegativeHumidity_throws() {
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.dewPoint(20.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.dewPoint(20.0, -1.0));
    }

    @Test
    void dewPoint_withHumidityAbove100_throws() {
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.dewPoint(20.0, 100.1));
    }

    @Test
    void dewPoint_withNaNInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.dewPoint(20.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.dewPoint(Double.NaN, 50.0));
    }

    @Test
    void absoluteHumidity_at20DegreesAnd50Percent_isAbout8Point6() {
        assertEquals(8.6, BiometeorologyCalculator.absoluteHumidity(20.0, 50.0), DELTA);
    }

    @Test
    void absoluteHumidity_atZeroHumidity_isZero() {
        assertEquals(0.0, BiometeorologyCalculator.absoluteHumidity(20.0, 0.0), 1e-9);
    }

    @Test
    void absoluteHumidity_at100PercentAnd20Degrees_isAbout17Point3() {
        assertEquals(17.3, BiometeorologyCalculator.absoluteHumidity(20.0, 100.0), DELTA);
    }

    @Test
    void absoluteHumidity_atNegativeTemperature_isSmallAndPositive() {
        assertEquals(1.9, BiometeorologyCalculator.absoluteHumidity(-10.0, 80.0), DELTA);
    }

    @Test
    void absoluteHumidity_withInvalidHumidity_throws() {
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.absoluteHumidity(20.0, -0.1));
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.absoluteHumidity(20.0, 101.0));
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.absoluteHumidity(20.0, Double.NaN));
    }

    @Test
    void heatIndex_at32DegreesAnd60Percent_isAbout38() {
        final Optional<Double> hi = BiometeorologyCalculator.heatIndex(32.0, 60.0);
        assertTrue(hi.isPresent());
        assertEquals(38.0, hi.get(), 1.0);
    }

    @Test
    void heatIndex_at35DegreesAnd70Percent_isHigherThanTemperature() {
        final Optional<Double> hi = BiometeorologyCalculator.heatIndex(35.0, 70.0);
        assertTrue(hi.isPresent());
        assertTrue(hi.get() > 35.0);
    }

    @Test
    void heatIndex_belowTemperatureThreshold_isEmpty() {
        assertTrue(BiometeorologyCalculator.heatIndex(26.6, 80.0).isEmpty());
        assertTrue(BiometeorologyCalculator.heatIndex(20.0, 60.0).isEmpty());
    }

    @Test
    void heatIndex_belowHumidityThreshold_isEmpty() {
        assertTrue(BiometeorologyCalculator.heatIndex(32.0, 39.9).isEmpty());
    }

    @Test
    void heatIndex_atThresholds_isPresent() {
        assertTrue(BiometeorologyCalculator.heatIndex(26.7, 40.0).isPresent());
    }

    @Test
    void heatIndex_withInvalidHumidity_throws() {
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.heatIndex(32.0, 100.5));
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.heatIndex(32.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> BiometeorologyCalculator.heatIndex(Double.NaN, 60.0));
    }
}
