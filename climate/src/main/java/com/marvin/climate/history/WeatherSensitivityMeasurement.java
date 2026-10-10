package com.marvin.climate.history;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.marvin.climate.dto.WeatherSensitivity;
import java.time.Instant;

/**
 * InfluxDB measurement holding the weather sensitivity level and trend as numbers.
 *
 * @param level the level (KEINE=0, GERING=1, MITTEL=2, HOCH=3)
 * @param trend the trend (FALLING=-1, STABLE=0, RISING=1)
 * @param time  the time of the assessment
 */
@Measurement(name = "weather_sensitivity")
public record WeatherSensitivityMeasurement(
        @Column Integer level,
        @Column Integer trend,
        @Column(timestamp = true) Instant time
) {

    /**
     * Creates a measurement from an assessment.
     *
     * @param sensitivity the assessment
     * @param time        the measurement time
     * @return the measurement
     */
    public static WeatherSensitivityMeasurement from(final WeatherSensitivity sensitivity, final Instant time) {
        return new WeatherSensitivityMeasurement(levelValue(sensitivity.level()), trendValue(sensitivity.trend()), time);
    }

    private static int levelValue(final WeatherSensitivity.Level level) {
        return switch (level) {
            case KEINE -> 0;
            case GERING -> 1;
            case MITTEL -> 2;
            case HOCH -> 3;
        };
    }

    private static int trendValue(final WeatherSensitivity.Trend trend) {
        return switch (trend) {
            case FALLING -> -1;
            case STABLE -> 0;
            case RISING -> 1;
        };
    }
}
