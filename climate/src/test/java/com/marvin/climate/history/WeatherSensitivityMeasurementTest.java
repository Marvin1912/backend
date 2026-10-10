package com.marvin.climate.history;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marvin.climate.dto.WeatherSensitivity;
import com.marvin.climate.dto.WeatherSensitivity.Level;
import com.marvin.climate.dto.WeatherSensitivity.Trend;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherSensitivityMeasurementTest {

    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

    private WeatherSensitivity assessment(final Level level, final Trend trend) {
        return new WeatherSensitivity(level, trend, List.of(), null);
    }

    @Test
    void mapsAllLevels() {
        assertEquals(0, WeatherSensitivityMeasurement.from(assessment(Level.KEINE, Trend.STABLE), NOW).level());
        assertEquals(1, WeatherSensitivityMeasurement.from(assessment(Level.GERING, Trend.STABLE), NOW).level());
        assertEquals(2, WeatherSensitivityMeasurement.from(assessment(Level.MITTEL, Trend.STABLE), NOW).level());
        assertEquals(3, WeatherSensitivityMeasurement.from(assessment(Level.HOCH, Trend.STABLE), NOW).level());
    }

    @Test
    void mapsAllTrends() {
        assertEquals(-1, WeatherSensitivityMeasurement.from(assessment(Level.KEINE, Trend.FALLING), NOW).trend());
        assertEquals(0, WeatherSensitivityMeasurement.from(assessment(Level.KEINE, Trend.STABLE), NOW).trend());
        assertEquals(1, WeatherSensitivityMeasurement.from(assessment(Level.KEINE, Trend.RISING), NOW).trend());
    }

    @Test
    void keepsTimestamp() {
        assertEquals(NOW, WeatherSensitivityMeasurement.from(assessment(Level.MITTEL, Trend.RISING), NOW).time());
    }
}
