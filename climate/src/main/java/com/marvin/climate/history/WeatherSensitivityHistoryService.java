package com.marvin.climate.history;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.marvin.climate.service.WeatherSensitivityService;
import com.marvin.influxdb.core.GenericPojoImporter;
import com.marvin.influxdb.core.InfluxWriteConfig;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Persists the current weather sensitivity assessment (level and trend) to InfluxDB.
 */
@Service
public class WeatherSensitivityHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherSensitivityHistoryService.class);

    private final WeatherSensitivityService weatherSensitivityService;
    private final GenericPojoImporter<WeatherSensitivityMeasurement> importer;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param weatherSensitivityService the assessment source
     * @param influxDBClient            the InfluxDB client
     * @param org                       the InfluxDB organization
     * @param bucket                    the target bucket
     */
    @Autowired
    public WeatherSensitivityHistoryService(
            final WeatherSensitivityService weatherSensitivityService,
            final InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") final String org,
            @Value("${climate.weather-sensitivity.history.bucket:sensor_data}") final String bucket
    ) {
        this(weatherSensitivityService, influxDBClient, org, bucket, Clock.systemUTC());
    }

    WeatherSensitivityHistoryService(
            final WeatherSensitivityService weatherSensitivityService,
            final InfluxDBClient influxDBClient,
            final String org,
            final String bucket,
            final Clock clock
    ) {
        this.weatherSensitivityService = weatherSensitivityService;
        this.importer = new GenericPojoImporter<>(influxDBClient, InfluxWriteConfig.create(bucket, org, WritePrecision.S));
        this.clock = clock;
    }

    /**
     * Records the current assessment. Errors are logged and swallowed.
     *
     * @return a Mono completing empty once recorded or after a swallowed failure
     */
    public Mono<Void> record() {
        return weatherSensitivityService.getWeatherSensitivity()
                .map(sensitivity -> WeatherSensitivityMeasurement.from(sensitivity, clock.instant()))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(measurement -> {
                    importer.importPojo(measurement);
                    LOGGER.info("Recorded weather sensitivity level={} trend={}", measurement.level(), measurement.trend());
                })
                .onErrorResume(e -> {
                    LOGGER.warn("Failed to record weather sensitivity", e);
                    return Mono.empty();
                })
                .then();
    }
}
