package com.marvin.climate.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.marvin.climate.biometeorology.BiometeorologyCalculator;
import com.marvin.climate.dto.ClimateTrend;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service reading historic temperature, dew point and pressure values from InfluxDB and deriving
 * the 3 h / 24 h deltas used for the weather sensitivity trend.
 */
@Service
public class ClimateTrendService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClimateTrendService.class);
    private static final String BUCKET = "sensor_data";
    private static final String TEMPERATURE_MEASUREMENT = "°C";
    private static final String HUMIDITY_MEASUREMENT = "%";
    private static final String PRESSURE_MEASUREMENT = "hPa";
    private static final Duration NOW = Duration.ZERO;
    private static final Duration THREE_HOURS = Duration.ofHours(3);
    private static final Duration ONE_DAY = Duration.ofHours(24);
    private static final Duration LOOKBACK_WINDOW = Duration.ofHours(1);
    private static final List<String> PRESSURE_ENTITY_IDS = List.of(
            "wohnzimmer_pressure",
            "flur_pressure",
            "schlafzimmer_pressure",
            "kueche_pressure",
            "badezimmer_pressure"
    );

    private final InfluxDBClient influxDBClient;
    private final String org;
    private final String outdoorEntityId;
    private final String outdoorHumidityEntityId;

    /**
     * Constructs a ClimateTrendService with required dependencies and configuration.
     *
     * @param influxDBClient          the InfluxDB client used to query sensor data
     * @param org                     the InfluxDB organisation name
     * @param outdoorEntityId         the entity ID for the outdoor temperature sensor
     * @param outdoorHumidityEntityId the entity ID for the outdoor humidity sensor
     */
    public ClimateTrendService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org,
            @Value("${climate.outdoor.entity-id:draussen_temperature}") String outdoorEntityId,
            @Value("${climate.outdoor.humidity-entity-id:draussen_humidity}") String outdoorHumidityEntityId
    ) {
        this.influxDBClient = influxDBClient;
        this.org = org;
        this.outdoorEntityId = outdoorEntityId;
        this.outdoorHumidityEntityId = outdoorHumidityEntityId;
    }

    /**
     * Determines the current outdoor temperature and dew point, the median indoor pressure and their
     * changes over the last 3 h (and 24 h for the temperature). Values that are not available, for
     * example because of a data gap or because no pressure sensor reports to InfluxDB, are left empty.
     *
     * @return a Mono emitting the {@link ClimateTrend}; it never fails because of missing data
     */
    public Mono<ClimateTrend> getTrend() {
        LOGGER.info("Fetching climate trend values from InfluxDB");

        final Mono<Optional<Double>> temperatureNow = temperatureAt(NOW);
        final Mono<Optional<Double>> temperature3h = temperatureAt(THREE_HOURS);
        final Mono<Optional<Double>> temperature24h = temperatureAt(ONE_DAY);
        final Mono<Optional<Double>> dewPointNow = dewPointAt(NOW);
        final Mono<Optional<Double>> dewPoint3h = dewPointAt(THREE_HOURS);
        final Mono<Optional<Double>> pressureNow = medianPressureAt(NOW);
        final Mono<Optional<Double>> pressure3h = medianPressureAt(THREE_HOURS);

        final Mono<List<Optional<Double>>> temperatures = Flux.concat(temperatureNow, temperature3h, temperature24h).collectList();
        final Mono<List<Optional<Double>>> dewPoints = Flux.concat(dewPointNow, dewPoint3h).collectList();
        final Mono<List<Optional<Double>>> pressures = Flux.concat(pressureNow, pressure3h).collectList();

        return Mono.zip(temperatures, dewPoints, pressures).map(tuple -> {
            final List<Double> t = tuple.getT1().stream().map(v -> v.orElse(null)).toList();
            final List<Double> d = tuple.getT2().stream().map(v -> v.orElse(null)).toList();
            final List<Double> p = tuple.getT3().stream().map(v -> v.orElse(null)).toList();
            return new ClimateTrend(
                    t.get(0),
                    delta(t.get(0), t.get(1)),
                    delta(t.get(0), t.get(2)),
                    d.get(0),
                    delta(d.get(0), d.get(1)),
                    p.get(0),
                    delta(p.get(0), p.get(1))
            );
        });
    }

    private Mono<Optional<Double>> temperatureAt(Duration offset) {
        return valueAt(TEMPERATURE_MEASUREMENT, outdoorEntityId, offset);
    }

    private Mono<Optional<Double>> dewPointAt(Duration offset) {
        return Mono.zip(temperatureAt(offset), valueAt(HUMIDITY_MEASUREMENT, outdoorHumidityEntityId, offset))
                .map(tuple -> {
                    if (tuple.getT1().isEmpty() || tuple.getT2().isEmpty()) {
                        return Optional.<Double>empty();
                    }
                    try {
                        return Optional.of(BiometeorologyCalculator.dewPoint(tuple.getT1().get(), tuple.getT2().get()));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Dew point not computable for temperature {} and humidity {}", tuple.getT1().get(), tuple.getT2().get());
                        return Optional.<Double>empty();
                    }
                });
    }

    private Mono<Optional<Double>> medianPressureAt(Duration offset) {
        return Flux.fromIterable(PRESSURE_ENTITY_IDS)
                .concatMap(entityId -> valueAt(PRESSURE_MEASUREMENT, entityId, offset))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collectList()
                .map(ClimateTrendService::median);
    }

    private static Optional<Double> median(List<Double> values) {
        if (values.isEmpty()) {
            return Optional.empty();
        }
        final List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        final int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return Optional.of(sorted.get(middle));
        }
        return Optional.of((sorted.get(middle - 1) + sorted.get(middle)) / 2.0);
    }

    private static Double delta(Double current, Double earlier) {
        if (current == null || earlier == null) {
            return null;
        }
        return current - earlier;
    }

    private Mono<Optional<Double>> valueAt(String measurement, String entityId, Duration offset) {
        final String flux = buildFluxQuery(measurement, entityId, offset);
        return Mono.fromCallable(() -> influxDBClient.getQueryApi().query(flux, org))
                .subscribeOn(Schedulers.boundedElastic())
                .map(tables -> tables.stream()
                        .flatMap(table -> table.getRecords().stream())
                        .findFirst()
                        .map(FluxRecord::getValue)
                        .map(value -> ((Number) value).doubleValue()))
                .onErrorResume(e -> {
                    LOGGER.warn("Trend query failed for entity_id '{}' (offset {}) - treating as missing", entityId, offset, e);
                    return Mono.just(Optional.empty());
                });
    }

    private String buildFluxQuery(String measurement, String entityId, Duration offset) {
        final long offsetMinutes = offset.toMinutes();
        final String range = offsetMinutes == 0
                ? String.format("range(start: -%dm)", LOOKBACK_WINDOW.toMinutes())
                : String.format("range(start: -%dm, stop: -%dm)", offsetMinutes + LOOKBACK_WINDOW.toMinutes(), offsetMinutes);
        return String.format(
                "from(bucket: \"%s\")"
                + " |> %s"
                + " |> filter(fn: (r) => r._measurement == \"%s\" and r.entity_id == \"%s\" and r._field == \"value\")"
                + " |> last()",
                BUCKET, range, measurement, entityId
        );
    }
}
