package com.marvin.climate.biometeorology;

import java.util.Optional;

/**
 * Stateless calculations of biometeorological metrics (dew point, absolute humidity, heat index).
 */
public final class BiometeorologyCalculator {

    private static final double MAGNUS_A = 17.62;
    private static final double MAGNUS_B = 243.12;

    private static final double SVP_A = 6.112;
    private static final double SVP_B = 17.67;
    private static final double SVP_C = 243.5;
    private static final double ABS_HUMIDITY_FACTOR = 2.1674;
    private static final double KELVIN_OFFSET = 273.15;

    private static final double HEAT_INDEX_MIN_TEMPERATURE_C = 26.7;
    private static final double HEAT_INDEX_MIN_HUMIDITY = 40.0;

    private static final double MAX_HUMIDITY = 100.0;

    private BiometeorologyCalculator() {
    }

    /**
     * Calculates the dew point using the Magnus formula.
     *
     * @param temperatureCelsius air temperature in degrees Celsius
     * @param relativeHumidity   relative humidity in percent, in the range (0, 100]
     * @return the dew point in degrees Celsius
     * @throws IllegalArgumentException if an input is NaN or the humidity is outside (0, 100]
     */
    public static double dewPoint(final double temperatureCelsius, final double relativeHumidity) {
        validateTemperature(temperatureCelsius);
        if (Double.isNaN(relativeHumidity) || relativeHumidity <= 0.0 || relativeHumidity > MAX_HUMIDITY) {
            throw new IllegalArgumentException("Relative humidity must be in (0, 100] for the dew point: " + relativeHumidity);
        }
        final double gamma = Math.log(relativeHumidity / MAX_HUMIDITY) + MAGNUS_A * temperatureCelsius / (MAGNUS_B + temperatureCelsius);
        return MAGNUS_B * gamma / (MAGNUS_A - gamma);
    }

    /**
     * Calculates the relative humidity from air temperature and dew point (inverse Magnus formula).
     * A dew point above the temperature is physically implausible for measured values and yields 100 %.
     *
     * @param temperatureCelsius air temperature in degrees Celsius
     * @param dewPointCelsius    dew point in degrees Celsius
     * @return the relative humidity in percent, in the range (0, 100]
     * @throws IllegalArgumentException if an input is NaN
     */
    public static double relativeHumidity(final double temperatureCelsius, final double dewPointCelsius) {
        validateTemperature(temperatureCelsius);
        validateTemperature(dewPointCelsius);
        final double exponent = MAGNUS_A * dewPointCelsius / (MAGNUS_B + dewPointCelsius)
                - MAGNUS_A * temperatureCelsius / (MAGNUS_B + temperatureCelsius);
        return Math.min(MAX_HUMIDITY, MAX_HUMIDITY * Math.exp(exponent));
    }

    /**
     * Calculates the absolute humidity (water vapour mass per air volume).
     *
     * @param temperatureCelsius air temperature in degrees Celsius
     * @param relativeHumidity   relative humidity in percent, in the range [0, 100]
     * @return the absolute humidity in g/m³
     * @throws IllegalArgumentException if an input is NaN or the humidity is outside [0, 100]
     */
    public static double absoluteHumidity(final double temperatureCelsius, final double relativeHumidity) {
        validateTemperature(temperatureCelsius);
        validateHumidity(relativeHumidity);
        final double saturationPressure = SVP_A * Math.exp(SVP_B * temperatureCelsius / (temperatureCelsius + SVP_C));
        return saturationPressure * relativeHumidity * ABS_HUMIDITY_FACTOR / (KELVIN_OFFSET + temperatureCelsius);
    }

    /**
     * Calculates the heat index using the Rothfusz (NOAA) regression.
     *
     * <p>The regression is only valid for temperatures of at least 26.7 °C and relative humidity of at least 40 %.
     * Outside this range an empty result is returned (the perceived temperature is then approximately the air temperature).</p>
     *
     * @param temperatureCelsius air temperature in degrees Celsius
     * @param relativeHumidity   relative humidity in percent, in the range [0, 100]
     * @return the heat index in degrees Celsius, or empty if outside the validity range
     * @throws IllegalArgumentException if an input is NaN or the humidity is outside [0, 100]
     */
    public static Optional<Double> heatIndex(final double temperatureCelsius, final double relativeHumidity) {
        validateTemperature(temperatureCelsius);
        validateHumidity(relativeHumidity);
        if (temperatureCelsius < HEAT_INDEX_MIN_TEMPERATURE_C || relativeHumidity < HEAT_INDEX_MIN_HUMIDITY) {
            return Optional.empty();
        }
        final double t = temperatureCelsius * 9.0 / 5.0 + 32.0;
        final double rh = relativeHumidity;
        final double hiF = -42.379
                + 2.04901523 * t
                + 10.14333127 * rh
                - 0.22475541 * t * rh
                - 6.83783e-3 * t * t
                - 5.481717e-2 * rh * rh
                + 1.22874e-3 * t * t * rh
                + 8.5282e-4 * t * rh * rh
                - 1.99e-6 * t * t * rh * rh;
        return Optional.of((hiF - 32.0) * 5.0 / 9.0);
    }

    private static void validateTemperature(final double temperatureCelsius) {
        if (Double.isNaN(temperatureCelsius)) {
            throw new IllegalArgumentException("Temperature must not be NaN");
        }
    }

    private static void validateHumidity(final double relativeHumidity) {
        if (Double.isNaN(relativeHumidity) || relativeHumidity < 0.0 || relativeHumidity > MAX_HUMIDITY) {
            throw new IllegalArgumentException("Relative humidity must be in [0, 100]: " + relativeHumidity);
        }
    }
}
