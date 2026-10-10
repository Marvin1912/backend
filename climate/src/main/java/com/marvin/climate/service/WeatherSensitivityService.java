package com.marvin.climate.service;

import com.marvin.climate.biometeorology.BiometeorologyCalculator;
import com.marvin.climate.dto.ClimateTrend;
import com.marvin.climate.dto.WeatherSensitivity;
import com.marvin.climate.dto.WeatherSensitivity.Level;
import com.marvin.climate.dto.WeatherSensitivity.Metrics;
import com.marvin.climate.dto.WeatherSensitivity.Trend;
import com.marvin.climate.weather.HourlyWeatherForecast;
import com.marvin.climate.weather.MetNoForecastClient;
import com.marvin.climate.weather.OpenWeatherMapClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Computes the weather sensitivity traffic light. This is a tendency heuristic and explicitly not a medical statement.
 *
 * <p>The overall level is the maximum of all individual signals. Every triggering signal adds a German text to the reasons.
 * Thresholds (magnitudes; drops are evaluated as positive magnitudes):</p>
 * <table>
 *   <caption>Signal thresholds</caption>
 *   <tr><th>Signal</th><th>GERING</th><th>MITTEL</th><th>HOCH</th></tr>
 *   <tr><td>Temperature drop 3 h (K)</td><td>&gt;= 3</td><td>-</td><td>&gt;= 5 (Kaltfront)</td></tr>
 *   <tr><td>Temperature drop 24 h (K)</td><td>&gt;= 5</td><td>&gt;= 8</td><td>-</td></tr>
 *   <tr><td>Forecast temperature drop (K)</td><td>&gt;= 3</td><td>-</td><td>&gt;= 5 (Kaltfront)</td></tr>
 *   <tr><td>Pressure drop 3 h or forecast (hPa)</td><td>&gt;= 0.5</td><td>&gt;= 1</td><td>&gt;= 3</td></tr>
 *   <tr><td>Outdoor dew point (degrees C)</td><td>&gt;= 14</td><td>&gt;= 16 (schwuel)</td><td>&gt;= 20</td></tr>
 *   <tr><td>Heat index (degrees C, valid range only)</td><td>-</td><td>&gt;= 27</td><td>&gt;= 32</td></tr>
 *   <tr><td>Dew point rise 3 h or forecast (K)</td><td>&gt;= 2</td><td>&gt;= 3</td><td>-</td></tr>
 * </table>
 *
 * <p>The trend compares the level of the measured signals with the level of the forecast-only signals (forecast
 * temperature and pressure change and dew point rise within the next few hours; met.no hourly
 * data is the primary source, OpenWeatherMap the fallback when met.no fails or returns nothing): a stronger forecast
 * level is {@link Trend#RISING}, a weaker one {@link Trend#FALLING}, an equal one {@link Trend#STABLE}.
 * Without forecast data the trend is {@link Trend#STABLE}.</p>
 *
 * <p>The forecast temperature change is the last forecast entry minus the current outdoor temperature (or the first
 * forecast entry when no outdoor temperature is known). The forecast pressure change is the last minus the first
 * forecast entry, as forecast pressure is a sea-level value and not comparable with the indoor sensors.</p>
 *
 * <p>Unavailable data sources lower the significance but never cause an error.</p>
 */
@Service
public class WeatherSensitivityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherSensitivityService.class);
    private static final Locale GERMAN = Locale.GERMAN;

    private static final double TEMP_DROP_GERING = 3.0;
    private static final double TEMP_DROP_HOCH = 5.0;
    private static final double TEMP_DROP_24H_GERING = 5.0;
    private static final double TEMP_DROP_24H_MITTEL = 8.0;
    private static final double PRESSURE_DROP_GERING = 0.5;
    private static final double PRESSURE_DROP_MITTEL = 1.0;
    private static final double PRESSURE_DROP_HOCH = 3.0;
    private static final double DEW_POINT_GERING = 14.0;
    private static final double DEW_POINT_MITTEL = 16.0;
    private static final double DEW_POINT_HOCH = 20.0;
    private static final double HEAT_INDEX_MITTEL = 27.0;
    private static final double HEAT_INDEX_HOCH = 32.0;
    private static final double DEW_RISE_GERING = 2.0;
    private static final double DEW_RISE_MITTEL = 3.0;
    private static final double UNREACHABLE = Double.POSITIVE_INFINITY;

    private final ClimateTrendService climateTrendService;
    private final OpenWeatherMapClient openWeatherMapClient;
    private final MetNoForecastClient metNoForecastClient;

    /**
     * Constructs the service with OpenWeatherMap as the only forecast source.
     *
     * @param climateTrendService  the service providing measured temperature, dew point and pressure changes
     * @param openWeatherMapClient the client providing the OpenWeatherMap forecast
     */
    public WeatherSensitivityService(ClimateTrendService climateTrendService, OpenWeatherMapClient openWeatherMapClient) {
        this(climateTrendService, openWeatherMapClient, null);
    }

    /**
     * Constructs the service with met.no as primary and OpenWeatherMap as fallback forecast source.
     *
     * @param climateTrendService  the service providing measured temperature, dew point and pressure changes
     * @param openWeatherMapClient the client providing the OpenWeatherMap fallback forecast
     * @param metNoForecastClient  the client providing the hourly met.no forecast, or {@code null} to use OpenWeatherMap only
     */
    @Autowired
    public WeatherSensitivityService(ClimateTrendService climateTrendService, OpenWeatherMapClient openWeatherMapClient,
            MetNoForecastClient metNoForecastClient) {
        this.climateTrendService = climateTrendService;
        this.openWeatherMapClient = openWeatherMapClient;
        this.metNoForecastClient = metNoForecastClient;
    }

    /**
     * Computes the current weather sensitivity assessment.
     *
     * @return a Mono emitting the assessment; failing or empty data sources are treated as missing data
     */
    public Mono<WeatherSensitivity> getWeatherSensitivity() {
        final Mono<ClimateTrend> trend = climateTrendService.getTrend()
                .onErrorResume(e -> {
                    LOGGER.warn("Climate trend unavailable for weather sensitivity", e);
                    return Mono.empty();
                })
                .defaultIfEmpty(new ClimateTrend(null, null, null, null, null, null, null));
        return Mono.zip(trend, loadForecast()).map(tuple -> evaluate(tuple.getT1(), tuple.getT2()));
    }

    private Mono<List<HourlyWeatherForecast>> loadForecast() {
        final Mono<List<HourlyWeatherForecast>> openWeatherMap = Mono.defer(
                () -> collectSafely(openWeatherMapClient.getHourlyForecast(), "OpenWeatherMap"));
        if (metNoForecastClient == null) {
            return openWeatherMap;
        }
        return collectSafely(metNoForecastClient.getHourlyForecast(), "met.no")
                .filter(list -> !list.isEmpty())
                .switchIfEmpty(openWeatherMap);
    }

    private static Mono<List<HourlyWeatherForecast>> collectSafely(Flux<HourlyWeatherForecast> source, String name) {
        return source.collectList()
                .onErrorResume(e -> {
                    LOGGER.warn("{} weather forecast unavailable for weather sensitivity", name, e);
                    return Mono.just(List.of());
                });
    }

    private WeatherSensitivity evaluate(ClimateTrend trend, List<HourlyWeatherForecast> forecasts) {
        final Double forecastDeltaT = forecastDeltaTemperature(trend, forecasts);
        final Double forecastDeltaPressure = forecastDeltaPressure(forecasts);
        final Double forecastDeltaDewPoint = forecastDeltaDewPoint(trend, forecasts);
        final Double relativeHumidity = relativeHumidity(trend);
        final Double heatIndex = heatIndex(trend, relativeHumidity);

        final List<Signal> measured = new ArrayList<>();
        measured.add(temperatureDrop(trend.temperatureDelta3h(), " in 3 h", TEMP_DROP_GERING, TEMP_DROP_HOCH, true));
        measured.add(temperatureDrop24h(trend.temperatureDelta24h()));
        measured.add(pressureDrop(trend.pressureDelta3h(), " in 3 h"));
        measured.add(dewPointLevel(trend.dewPointC()));
        measured.add(heatIndexSignal(heatIndex));
        measured.add(dewPointRise(trend.dewPointDelta3h(), " in 3 h"));

        final List<Signal> forecastSignals = List.of(
                temperatureDrop(forecastDeltaT, " laut Prognose", TEMP_DROP_GERING, TEMP_DROP_HOCH, true),
                pressureDrop(forecastDeltaPressure, " laut Prognose"),
                dewPointRise(forecastDeltaDewPoint, " laut Prognose"));

        final List<Signal> all = new ArrayList<>(measured);
        all.addAll(forecastSignals);

        final List<String> reasons = all.stream().filter(s -> s.level() != Level.KEINE).map(Signal::reason).toList();
        final Level level = maxLevel(all);
        final Trend resultTrend = forecasts.isEmpty() ? Trend.STABLE : compareTrend(maxLevel(measured), maxLevel(forecastSignals));
        final Double absoluteHumidity = absoluteHumidity(trend, relativeHumidity);
        final Metrics metrics = new Metrics(round(trend.dewPointC()), round(absoluteHumidity), round(heatIndex),
                round(trend.temperatureDelta3h()), round(trend.temperatureDelta24h()), round(trend.dewPointDelta3h()),
                round(trend.pressureDelta3h()), round(forecastDeltaT), round(forecastDeltaPressure));
        return new WeatherSensitivity(level, resultTrend, reasons, metrics);
    }

    private static Trend compareTrend(Level measured, Level forecast) {
        final int comparison = forecast.compareTo(measured);
        if (comparison > 0) {
            return Trend.RISING;
        }
        return comparison < 0 ? Trend.FALLING : Trend.STABLE;
    }

    private static Level maxLevel(List<Signal> signals) {
        return signals.stream().map(Signal::level).max(Comparator.naturalOrder()).orElse(Level.KEINE);
    }

    // ---- signals ----

    private static Signal temperatureDrop(Double delta, String suffix, double gering, double hoch, boolean frontLabel) {
        if (delta == null) {
            return Signal.none();
        }
        final Level level = classify(-delta, gering, UNREACHABLE, hoch);
        if (level == Level.KEINE) {
            return Signal.none();
        }
        final String label = level == Level.HOCH && frontLabel ? "Temperatursturz" : "Temperaturrückgang";
        final String text = label + " " + format(delta) + " K" + suffix + (level == Level.HOCH && frontLabel ? " (Kaltfront)" : "");
        return new Signal(level, text);
    }

    private static Signal temperatureDrop24h(Double delta) {
        if (delta == null) {
            return Signal.none();
        }
        final Level level = classify(-delta, TEMP_DROP_24H_GERING, TEMP_DROP_24H_MITTEL, UNREACHABLE);
        return level == Level.KEINE ? Signal.none() : new Signal(level, "Temperaturrückgang " + format(delta) + " K in 24 h");
    }

    private static Signal pressureDrop(Double delta, String suffix) {
        if (delta == null) {
            return Signal.none();
        }
        final Level level = classify(-delta, PRESSURE_DROP_GERING, PRESSURE_DROP_MITTEL, PRESSURE_DROP_HOCH);
        return level == Level.KEINE ? Signal.none() : new Signal(level, "Luftdruckabfall " + format(delta) + " hPa" + suffix);
    }

    private static Signal dewPointLevel(Double dewPoint) {
        if (dewPoint == null) {
            return Signal.none();
        }
        final Level level = classify(dewPoint, DEW_POINT_GERING, DEW_POINT_MITTEL, DEW_POINT_HOCH);
        final String description = switch (level) {
            case HOCH -> " (sehr schwül)";
            case MITTEL -> " (schwül)";
            case GERING -> " (leicht feucht)";
            default -> "";
        };
        return level == Level.KEINE ? Signal.none() : new Signal(level, "Taupunkt " + formatPlain(dewPoint) + " °C" + description);
    }

    private static Signal heatIndexSignal(Double heatIndex) {
        if (heatIndex == null) {
            return Signal.none();
        }
        final Level level = classify(heatIndex, UNREACHABLE, HEAT_INDEX_MITTEL, HEAT_INDEX_HOCH);
        return level == Level.KEINE ? Signal.none() : new Signal(level, "Hitzeindex " + formatPlain(heatIndex) + " °C");
    }

    private static Signal dewPointRise(Double delta, String suffix) {
        if (delta == null) {
            return Signal.none();
        }
        final Level level = classify(delta, DEW_RISE_GERING, DEW_RISE_MITTEL, UNREACHABLE);
        return level == Level.KEINE ? Signal.none() : new Signal(level, "Taupunkt steigt schnell: " + format(delta) + " K" + suffix);
    }

    private static Level classify(double magnitude, double gering, double mittel, double hoch) {
        if (magnitude >= hoch) {
            return Level.HOCH;
        }
        if (magnitude >= mittel) {
            return Level.MITTEL;
        }
        return magnitude >= gering ? Level.GERING : Level.KEINE;
    }

    // ---- derived values ----

    private static Double forecastDeltaTemperature(ClimateTrend trend, List<HourlyWeatherForecast> forecasts) {
        if (forecasts.isEmpty()) {
            return null;
        }
        final double last = forecasts.get(forecasts.size() - 1).temperatureC();
        if (trend.temperatureC() != null) {
            return last - trend.temperatureC();
        }
        return forecasts.size() > 1 ? last - forecasts.get(0).temperatureC() : null;
    }

    private static Double forecastDeltaPressure(List<HourlyWeatherForecast> forecasts) {
        if (forecasts.size() < 2) {
            return null;
        }
        final Double first = forecasts.get(0).pressure();
        final Double last = forecasts.get(forecasts.size() - 1).pressure();
        return first == null || last == null ? null : last - first;
    }

    private static Double forecastDeltaDewPoint(ClimateTrend trend, List<HourlyWeatherForecast> forecasts) {
        if (forecasts.isEmpty()) {
            return null;
        }
        final Double last = forecasts.get(forecasts.size() - 1).dewPointC();
        if (last == null) {
            return null;
        }
        if (trend.dewPointC() != null) {
            return last - trend.dewPointC();
        }
        final Double first = forecasts.get(0).dewPointC();
        return forecasts.size() > 1 && first != null ? last - first : null;
    }

    private static Double relativeHumidity(ClimateTrend trend) {
        if (trend.temperatureC() == null || trend.dewPointC() == null) {
            return null;
        }
        try {
            return BiometeorologyCalculator.relativeHumidity(trend.temperatureC(), trend.dewPointC());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Double absoluteHumidity(ClimateTrend trend, Double relativeHumidity) {
        if (trend.temperatureC() == null || relativeHumidity == null) {
            return null;
        }
        try {
            return BiometeorologyCalculator.absoluteHumidity(trend.temperatureC(), relativeHumidity);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Double heatIndex(ClimateTrend trend, Double relativeHumidity) {
        if (trend.temperatureC() == null || relativeHumidity == null) {
            return null;
        }
        try {
            final Optional<Double> heatIndex = BiometeorologyCalculator.heatIndex(trend.temperatureC(), relativeHumidity);
            return heatIndex.orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- formatting ----

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 10.0) / 10.0;
    }

    private static String formatPlain(double value) {
        return String.format(GERMAN, "%.1f", value);
    }

    private static String format(double value) {
        final String text = formatPlain(value);
        return value > 0 ? "+" + text : text;
    }

    private record Signal(Level level, String reason) {

        static Signal none() {
            return new Signal(Level.KEINE, "");
        }
    }
}
