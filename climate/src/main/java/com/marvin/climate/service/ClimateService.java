package com.marvin.climate.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.marvin.climate.dto.TemperatureReading;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service responsible for querying climate sensor readings from InfluxDB.
 */
@Service
public class ClimateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClimateService.class);
    private static final String BUCKET = "sensor_data";
    private static final String TEMPERATURE_MEASUREMENT = "°C";
    private static final String HUMIDITY_MEASUREMENT = "%";
    private static final String OUTDOOR_LABEL = "Draußen";
    private static final String OUTDOOR_LOCATION = "outdoor";
    private static final FluxRecord MISSING_RECORD = new FluxRecord(-1);

    private final InfluxDBClient influxDBClient;
    private final String org;
    private final String outdoorEntityId;
    private final String outdoorHumidityEntityId;

    /**
     * Constructs a ClimateService with required dependencies and configuration.
     *
     * @param influxDBClient          the InfluxDB client used to query sensor data
     * @param org                     the InfluxDB organisation name
     * @param outdoorEntityId         the entity ID for the outdoor temperature sensor
     * @param outdoorHumidityEntityId the entity ID for the outdoor humidity sensor
     */
    public ClimateService(
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
     * Retrieves the current climate readings from all configured sensors, merging
     * temperature and humidity for the outdoor sensor into a single DTO.
     *
     * @return a Flux emitting one {@link TemperatureReading} per outdoor sensor record found in InfluxDB
     */
    public Flux<TemperatureReading> getCurrentReadings() {
        LOGGER.info("Fetching current climate readings from InfluxDB");

        final Mono<FluxRecord> temperatureMono = queryLatestRecord(
                buildFluxQuery(TEMPERATURE_MEASUREMENT, outdoorEntityId), outdoorEntityId);
        final Mono<FluxRecord> humidityMono = queryLatestRecord(
                buildFluxQuery(HUMIDITY_MEASUREMENT, outdoorHumidityEntityId), outdoorHumidityEntityId)
                .onErrorResume(e -> {
                    LOGGER.warn("Humidity query failed for entity_id '{}' - continuing without humidity", outdoorHumidityEntityId, e);
                    return Mono.just(MISSING_RECORD);
                });

        return Mono.zip(temperatureMono, humidityMono)
                .flatMapMany(tuple -> mergeReadings(tuple.getT1(), tuple.getT2()));
    }

    private Mono<FluxRecord> queryLatestRecord(String flux, String entityId) {
        return Mono.fromCallable(() -> influxDBClient.getQueryApi().query(flux, org))
                .subscribeOn(Schedulers.boundedElastic())
                .map(tables -> extractFirstRecord(tables, entityId))
                .doOnError(e -> LOGGER.error("Failed to fetch climate reading from InfluxDB for entity_id '{}'", entityId, e));
    }

    private FluxRecord extractFirstRecord(List<FluxTable> tables, String entityId) {
        if (tables.isEmpty()) {
            LOGGER.warn("InfluxDB query returned no records for entity_id '{}'", entityId);
            return MISSING_RECORD;
        }
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .findFirst()
                .orElse(MISSING_RECORD);
    }

    private Flux<TemperatureReading> mergeReadings(FluxRecord temperature, FluxRecord humidity) {
        if (temperature == MISSING_RECORD) {
            return Flux.empty();
        }
        final double temperatureC = ((Number) temperature.getValue()).doubleValue();
        final Double humidityPct = humidity == MISSING_RECORD
                ? null
                : ((Number) humidity.getValue()).doubleValue();
        return Flux.just(new TemperatureReading(
                outdoorEntityId,
                OUTDOOR_LABEL,
                OUTDOOR_LOCATION,
                temperatureC,
                humidityPct,
                temperature.getTime()
        ));
    }

    private String buildFluxQuery(String measurement, String entityId) {
        return String.format(
                "from(bucket: \"%s\")"
                + " |> range(start: -24h)"
                + " |> filter(fn: (r) => r._measurement == \"%s\" and r.entity_id == \"%s\" and r._field == \"value\")"
                + " |> last()",
                BUCKET, measurement, entityId
        );
    }
}
