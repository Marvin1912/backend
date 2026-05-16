package com.marvin.climate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClimateService Tests")
class ClimateServiceTest {

    private static final String TEST_ORG = "test_org";
    private static final String TEST_ENTITY_ID = "draussen_temperature";
    private static final double TEST_TEMPERATURE = 21.5;

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private QueryApi queryApi;

    private ClimateService climateService;

    @BeforeEach
    void setUp() {
        climateService = new ClimateService(influxDBClient, TEST_ORG, TEST_ENTITY_ID);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
    }

    @Test
    @DisplayName("Should emit one TemperatureReading for a single FluxRecord")
    void getCurrentReadings_ShouldEmitOneReading_WhenOneRecordReturned() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        final FluxTable table = buildTable(TEST_TEMPERATURE, measuredAt);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of(table));

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .assertNext(reading -> {
                    assertEquals(TEST_ENTITY_ID, reading.sensorId());
                    assertEquals("Draußen", reading.label());
                    assertEquals("outdoor", reading.location());
                    assertEquals(TEST_TEMPERATURE, reading.temperatureC());
                    assertEquals(measuredAt, reading.measuredAt());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty Flux and log warn when InfluxDB returns no tables")
    void getCurrentReadings_ShouldReturnEmpty_WhenNoTablesReturned() {
        // Given
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should include configured entity_id in the Flux query string")
    void getCurrentReadings_ShouldPassEntityIdInQuery() {
        // Given
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());
        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

        // When
        climateService.getCurrentReadings().blockLast();

        // Then
        verify(queryApi).query(queryCaptor.capture(), eq(TEST_ORG));
        assertTrue(queryCaptor.getValue().contains(TEST_ENTITY_ID), "Query must contain the configured entity_id");
    }

    @Test
    @DisplayName("Should propagate InfluxDB query errors")
    void getCurrentReadings_ShouldPropagateError_WhenInfluxFails() {
        // Given
        final RuntimeException influxFailure = new RuntimeException("influx down");
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenThrow(influxFailure);

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .expectErrorMatches(t -> t == influxFailure || t.getCause() == influxFailure)
                .verify();
    }

    private FluxTable buildTable(double temperature, Instant time) {
        final FluxTable table = new FluxTable();
        final FluxRecord record = new FluxRecord(0);
        record.getValues().put("_value", temperature);
        record.getValues().put("_time", time);
        table.getRecords().add(record);
        return table;
    }
}
