package com.marvin.plants.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.marvin.plants.dto.PlantMoistureReading;
import java.util.List;
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
     * caller to fail.
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
                + " |> range(start: -24h)"
                + " |> filter(fn: (r) => r._measurement == \"%s\" and r.entity_id == \"%s\" and r._field == \"value\")"
                + " |> last()",
                BUCKET, MOISTURE_MEASUREMENT, entityId
        );
    }
}
