package com.marvin.climate.dto;

import java.util.List;

/**
 * Ready-made weather sensitivity assessment ("Wetterfuehligkeits-Ampel"). This is a tendency heuristic and
 * explicitly not a medical statement.
 *
 * @param level   the overall level, the maximum of all individual signals
 * @param trend   whether the signal is expected to increase, stay the same or decrease
 * @param reasons German texts, one per triggering signal
 * @param metrics the underlying measured and forecast values; values that are unavailable are {@code null}
 */
public record WeatherSensitivity(Level level, Trend trend, List<String> reasons, Metrics metrics) {

    /**
     * Overall sensitivity level, ordered from lowest to highest.
     */
    public enum Level {
        /** No signal triggered. */
        KEINE,
        /** Weak signals only. */
        GERING,
        /** Noticeable signal. */
        MITTEL,
        /** Strong signal. */
        HOCH
    }

    /**
     * Expected development of the signal.
     */
    public enum Trend {
        /** The signal is expected to increase. */
        RISING,
        /** The signal is expected to stay the same. */
        STABLE,
        /** The signal is expected to decrease. */
        FALLING
    }

    /**
     * Values the assessment is based on, rounded to one decimal place.
     *
     * @param dewPointOutside         outdoor dew point in degrees Celsius
     * @param absoluteHumidityOutside outdoor absolute humidity in g/m3
     * @param heatIndexOutside        outdoor heat index in degrees Celsius, {@code null} outside its validity range
     * @param deltaT3h                outdoor temperature change over 3 h in K
     * @param deltaT24h               outdoor temperature change over 24 h in K
     * @param deltaDewPoint3h         outdoor dew point change over 3 h in K
     * @param deltaPressure3h         indoor pressure change over 3 h in hPa
     * @param forecastDeltaT          forecast temperature change relative to now in K
     * @param forecastDeltaPressure   forecast pressure change over the forecast window in hPa
     */
    public record Metrics(
            Double dewPointOutside,
            Double absoluteHumidityOutside,
            Double heatIndexOutside,
            Double deltaT3h,
            Double deltaT24h,
            Double deltaDewPoint3h,
            Double deltaPressure3h,
            Double forecastDeltaT,
            Double forecastDeltaPressure
    ) {
    }
}
