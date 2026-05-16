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
    private static final String MEASUREMENT = "°C";
    private static final String OUTDOOR_LABEL = "Draußen";
    private static final String OUTDOOR_LOCATION = "outdoor";

    private final InfluxDBClient influxDBClient;
    private final String org;
    private final String outdoorEntityId;

    /**
     * Constructs a ClimateService with required dependencies and configuration.
     *
     * @param influxDBClient  the InfluxDB client used to query sensor data
     * @param org             the InfluxDB organisation name
     * @param outdoorEntityId the entity ID for the outdoor temperature sensor
     */
    public ClimateService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org,
            @Value("${climate.outdoor.entity-id:draussen_temperature}") String outdoorEntityId
    ) {
        this.influxDBClient = influxDBClient;
        this.org = org;
        this.outdoorEntityId = outdoorEntityId;
    }

    /**
     * Retrieves the current temperature readings from all configured climate sensors.
     *
     * @return a Flux emitting one {@link TemperatureReading} per sensor record found in InfluxDB
     */
    public Flux<TemperatureReading> getCurrentReadings() {
        LOGGER.info("Fetching current climate readings from InfluxDB");
        final String flux = buildFluxQuery();

        return Mono.fromCallable(() -> influxDBClient.getQueryApi().query(flux, org))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(this::mapTablesToReadings);
    }

    private String buildFluxQuery() {
        return String.format(
                "from(bucket: \"%s\")"
                + " |> range(start: -15m)"
                + " |> filter(fn: (r) => r._measurement == \"%s\" and r.entity_id == \"%s\" and r._field == \"value\")"
                + " |> last()",
                BUCKET, MEASUREMENT, outdoorEntityId
        );
    }

    private Flux<TemperatureReading> mapTablesToReadings(List<FluxTable> tables) {
        if (tables.isEmpty()) {
            LOGGER.warn("InfluxDB query returned no records for entity_id '{}'", outdoorEntityId);
            return Flux.empty();
        }
        return Flux.fromIterable(tables)
                .flatMap(table -> Flux.fromIterable(table.getRecords()))
                .map(this::toTemperatureReading);
    }

    private TemperatureReading toTemperatureReading(FluxRecord record) {
        final double temperatureC = ((Number) record.getValue()).doubleValue();
        return new TemperatureReading(
                outdoorEntityId,
                OUTDOOR_LABEL,
                OUTDOOR_LOCATION,
                temperatureC,
                record.getTime()
        );
    }
}
