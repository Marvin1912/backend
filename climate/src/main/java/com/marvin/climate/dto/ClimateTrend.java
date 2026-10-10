package com.marvin.climate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Trend values needed for the weather sensitivity indicator. Any value that cannot be determined
 * (data gap, missing sensor) is {@code null} and never reported as 0.
 *
 * @param temperatureC       current outdoor temperature in degrees Celsius
 * @param temperatureDelta3h outdoor temperature change over the last 3 hours in K
 * @param temperatureDelta24h outdoor temperature change over the last 24 hours in K
 * @param dewPointC          current outdoor dew point in degrees Celsius
 * @param dewPointDelta3h    outdoor dew point change over the last 3 hours in K
 * @param pressureHpa        current median indoor air pressure in hPa
 * @param pressureDelta3h    median indoor air pressure change over the last 3 hours in hPa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClimateTrend(
        Double temperatureC,
        Double temperatureDelta3h,
        Double temperatureDelta24h,
        Double dewPointC,
        Double dewPointDelta3h,
        Double pressureHpa,
        Double pressureDelta3h
) {
}
