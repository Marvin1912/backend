package com.marvin.plants.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.marvin.plants.dto.PlantMoistureReading;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service responsible for querying the current soil moisture reading of a plant from InfluxDB.
 */
@Service
public class PlantMoistureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlantMoistureService.class);
    private static final String BUCKET = "sensor_data";
    private static final String MOISTURE_MEASUREMENT = "%";
    private static final String ENTITY_ID_COLUMN = "entity_id";
    private static final int SENSOR_LOOKUP_RANGE_DAYS = 30;

    private final InfluxDBClient influxDBClient;
    private final String org;

    /**
     * Constructs a PlantMoistureService with the required InfluxDB client and configuration.
     *
     * @param influxDBClient the InfluxDB client used to query sensor data
     * @param org            the InfluxDB organisation name
     */
    public PlantMoistureService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org
    ) {
        this.influxDBClient = influxDBClient;
        this.org = org;
    }

    /**
     * Retrieves the most recent soil moisture reading for the given plant. Emits an empty Mono
     * (without querying InfluxDB) when no {@code entityId} is configured, and also when InfluxDB
     * has no data or the query fails, so that a missing or misbehaving sensor never causes the
     * caller to fail. Queries a {@value #SENSOR_LOOKUP_RANGE_DAYS}-day range rather than a short
     * one, since Home Assistant only writes a new data point when a sensor's value actually
     * changes; a soil moisture sensor can therefore legitimately go unchanged (and thus silent)
     * for days between waterings without that being a sensor failure.
     *
     * @param plantName the name of the plant the reading belongs to
     * @param entityId  the InfluxDB {@code entity_id} of the plant's soil moisture sensor
     * @return a Mono emitting the current moisture reading, or empty if unavailable
     */
    public Mono<PlantMoistureReading> getCurrentMoisture(String plantName, String entityId) {
        if (entityId == null || entityId.isBlank()) {
            LOGGER.warn("No soil moisture entity_id configured for plant '{}'", plantName);
            return Mono.empty();
        }

        return queryLatestRecord(entityId)
                .map(record -> toReading(plantName, record))
                .onErrorResume(e -> {
                    LOGGER.error("Failed to fetch soil moisture from InfluxDB for entity_id '{}'", entityId, e);
                    return Mono.empty();
                });
    }

    private Mono<FluxRecord> queryLatestRecord(String entityId) {
        return Mono.fromCallable(() -> influxDBClient.getQueryApi().query(buildFluxQuery(entityId), org))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tables -> extractRecord(tables, entityId));
    }

    private Mono<FluxRecord> extractRecord(List<FluxTable> tables, String entityId) {
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .findFirst()
                .map(Mono::just)
                .orElseGet(() -> {
                    LOGGER.warn("InfluxDB query returned no records for entity_id '{}'", entityId);
                    return Mono.empty();
                });
    }

    private PlantMoistureReading toReading(String plantName, FluxRecord record) {
        final double moisturePercent = ((Number) record.getValue()).doubleValue();
        return new PlantMoistureReading(plantName, moisturePercent, record.getTime());
    }

    private String buildFluxQuery(String entityId) {
        return String.format(
                "from(bucket: \"%s\")"
                + " |> range(start: -%dd)"
                + " |> filter(fn: (r) => r._measurement == \"%s\" and r.entity_id == \"%s\" and r._field == \"value\")"
                + " |> last()",
                BUCKET, SENSOR_LOOKUP_RANGE_DAYS, MOISTURE_MEASUREMENT, entityId
        );
    }

    /**
     * Lists the {@code entity_id}s of all known soil moisture sensors found in InfluxDB, so that a
     * caller (e.g. a future UI) can offer them for assignment to a plant instead of requiring the
     * user to look them up manually. Filters out sibling sensors on the same "%" measurement, such
     * as room humidity ({@code *_humidity}) sensors, by matching only entity ids containing
     * "moisture". Never fails: returns an empty list when InfluxDB has no matching data or the
     * query fails.
     *
     * @return a Mono emitting the sorted, distinct list of available moisture sensor entity ids
     */
    public Mono<List<String>> listAvailableSensorEntityIds() {
        return Mono.fromCallable(() -> influxDBClient.getQueryApi().query(buildAvailableSensorsFluxQuery(), org))
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::extractSortedEntityIds)
                .onErrorResume(e -> {
                    LOGGER.error("Failed to fetch available moisture sensor entity_ids from InfluxDB", e);
                    return Mono.just(List.of());
                });
    }

    private List<String> extractSortedEntityIds(List<FluxTable> tables) {
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(record -> (String) record.getValue())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    private String buildAvailableSensorsFluxQuery() {
        return String.format(
                "from(bucket: \"%s\")"
                + " |> range(start: -%dd)"
                + " |> filter(fn: (r) => r._measurement == \"%s\" and r._field == \"value\")"
                + " |> filter(fn: (r) => r.%s =~ /moisture/)"
                + " |> keep(columns: [\"%s\"])"
                + " |> distinct(column: \"%s\")",
                BUCKET, SENSOR_LOOKUP_RANGE_DAYS, MOISTURE_MEASUREMENT, ENTITY_ID_COLUMN, ENTITY_ID_COLUMN, ENTITY_ID_COLUMN
        );
    }
}
