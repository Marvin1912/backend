package com.marvin.climate.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.marvin.climate.dto.WeatherSensitivity;
import com.marvin.climate.dto.WeatherSensitivity.Level;
import com.marvin.climate.dto.WeatherSensitivity.Trend;
import com.marvin.climate.service.WeatherSensitivityService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class WeatherSensitivityHistoryServiceTest {

    private static final String ORG = "org";
    private static final String BUCKET = "bucket";
    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

    @Mock
    private WeatherSensitivityService weatherSensitivityService;

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private WriteApiBlocking writeApi;

    private WeatherSensitivityHistoryService service;

    @BeforeEach
    void setUp() {
        service = new WeatherSensitivityHistoryService(
                weatherSensitivityService, influxDBClient, ORG, BUCKET, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordWritesOneMeasurement() {
        when(weatherSensitivityService.getWeatherSensitivity())
                .thenReturn(Mono.just(new WeatherSensitivity(Level.HOCH, Trend.FALLING, List.of("x"), null)));
        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApi);

        StepVerifier.create(service.record()).verifyComplete();

        verify(writeApi).writeMeasurement(BUCKET, ORG, WritePrecision.S, new WeatherSensitivityMeasurement(3, -1, NOW));
    }

    @Test
    void serviceErrorCompletesEmptyWithoutWrite() {
        when(weatherSensitivityService.getWeatherSensitivity()).thenReturn(Mono.error(new IllegalStateException("boom")));

        StepVerifier.create(service.record()).verifyComplete();

        verifyNoInteractions(influxDBClient);
    }

    @Test
    void writeErrorCompletesEmpty() {
        when(weatherSensitivityService.getWeatherSensitivity())
                .thenReturn(Mono.just(new WeatherSensitivity(Level.KEINE, Trend.STABLE, List.of(), null)));
        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApi);
        org.mockito.Mockito.doThrow(new RuntimeException("down")).when(writeApi)
                .writeMeasurement(any(String.class), any(String.class), any(WritePrecision.class), any());

        StepVerifier.create(service.record()).verifyComplete();

        verify(writeApi, never()).writeMeasurement(BUCKET, ORG, WritePrecision.S, (Object) null);
    }
}
