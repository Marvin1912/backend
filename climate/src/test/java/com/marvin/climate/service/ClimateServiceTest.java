package com.marvin.climate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.marvin.climate.dto.TemperatureReading;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private static final String TEST_HUMIDITY_ENTITY_ID = "draussen_humidity";
    private static final double TEST_TEMPERATURE = 21.5;
    private static final double TEST_HUMIDITY = 55.0;

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private QueryApi queryApi;

    private ClimateService climateService;

    @BeforeEach
    void setUp() {
        climateService = new ClimateService(influxDBClient, TEST_ORG, TEST_ENTITY_ID, TEST_HUMIDITY_ENTITY_ID);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
    }

    @Test
    @DisplayName("Should emit merged outdoor reading with temperature and humidity when both are present")
    void getCurrentReadings_ShouldMergeTemperatureAndHumidity_WhenBothPresent() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        final Instant humidityAt = Instant.parse("2026-05-16T10:00:05Z");
        stubOutdoor(
                List.of(buildTable(TEST_TEMPERATURE, measuredAt)),
                List.of(buildTable(TEST_HUMIDITY, humidityAt))
        );

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .assertNext(reading -> {
                    assertEquals(TEST_ENTITY_ID, reading.sensorId());
                    assertEquals("Draußen", reading.label());
                    assertEquals("outdoor", reading.location());
                    assertEquals(TEST_TEMPERATURE, reading.temperatureC());
                    assertEquals(TEST_HUMIDITY, reading.humidityPct());
                    assertEquals(measuredAt, reading.measuredAt());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit outdoor reading without humidity when humidity query returns no records")
    void getCurrentReadings_ShouldOmitHumidity_WhenHumidityMissing() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        stubOutdoor(
                List.of(buildTable(TEST_TEMPERATURE, measuredAt)),
                List.of()
        );

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .assertNext(reading -> {
                    assertEquals(TEST_TEMPERATURE, reading.temperatureC());
                    assertNull(reading.humidityPct());
                    assertEquals(measuredAt, reading.measuredAt());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit outdoor reading without humidity when humidity query fails")
    void getCurrentReadings_ShouldOmitHumidity_WhenHumidityQueryFails() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenAnswer(invocation -> {
            final String query = invocation.getArgument(0);
            if (query.contains(TEST_HUMIDITY_ENTITY_ID)) {
                throw new RuntimeException("humidity down");
            }
            if (query.contains(TEST_ENTITY_ID)) {
                return List.of(buildTable(TEST_TEMPERATURE, measuredAt));
            }
            return List.of();
        });

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .assertNext(reading -> {
                    assertEquals(TEST_TEMPERATURE, reading.temperatureC());
                    assertNull(reading.humidityPct());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty Flux when no sensors return data")
    void getCurrentReadings_ShouldReturnEmpty_WhenNoSensorsHaveData() {
        // Given
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should include configured outdoor entity IDs in the Flux queries")
    void getCurrentReadings_ShouldPassEntityIdsInQueries() {
        // Given
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());
        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

        // When
        climateService.getCurrentReadings().blockLast();

        // Then
        verify(queryApi, atLeast(2)).query(queryCaptor.capture(), eq(TEST_ORG));
        final List<String> queries = queryCaptor.getAllValues();
        assertTrue(queries.stream().anyMatch(q -> q.contains(TEST_ENTITY_ID)), "Query must contain the temperature entity_id");
        assertTrue(queries.stream().anyMatch(q -> q.contains(TEST_HUMIDITY_ENTITY_ID)), "Query must contain the humidity entity_id");
    }

    @Test
    @DisplayName("Should skip outdoor reading when temperature query fails but still emit other readings")
    void getCurrentReadings_ShouldSkipOutdoor_WhenTemperatureQueryFails() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenAnswer(invocation -> {
            final String query = invocation.getArgument(0);
            if (query.contains(TEST_ENTITY_ID) || query.contains(TEST_HUMIDITY_ENTITY_ID)) {
                throw new RuntimeException("outdoor down");
            }
            if (query.contains("kueche_temperature")) {
                return List.of(buildTable(22.2, measuredAt));
            }
            return List.of();
        });

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .assertNext(reading -> {
                    assertEquals("kueche_temperature", reading.sensorId());
                    assertEquals("Küche", reading.label());
                    assertEquals("indoor", reading.location());
                    assertEquals(22.2, reading.temperatureC());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit one indoor reading per configured room with data")
    void getCurrentReadings_ShouldEmitIndoorReadings_ForEachRoom() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        final Map<String, Double> indoorTemps = Map.of(
                "badezimmer_temperature", 23.1,
                "flur_temperature", 21.2,
                "kueche_temperature", 22.3,
                "schlafzimmer_temperature", 20.4,
                "wohnzimmer_temperature", 22.5
        );
        final Map<String, Double> indoorHumidities = Map.of(
                "badezimmer_humidity", 65.0,
                "flur_humidity", 45.0,
                "kueche_humidity", 50.0,
                "schlafzimmer_humidity", 48.0,
                "wohnzimmer_humidity", 46.0
        );
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenAnswer(invocation -> {
            final String query = invocation.getArgument(0);
            for (final Map.Entry<String, Double> entry : indoorTemps.entrySet()) {
                if (query.contains(entry.getKey())) {
                    return List.of(buildTable(entry.getValue(), measuredAt));
                }
            }
            for (final Map.Entry<String, Double> entry : indoorHumidities.entrySet()) {
                if (query.contains(entry.getKey())) {
                    return List.of(buildTable(entry.getValue(), measuredAt));
                }
            }
            return List.of();
        });

        // When / Then
        StepVerifier.create(climateService.getCurrentReadings())
                .recordWith(java.util.ArrayList::new)
                .expectNextCount(5)
                .consumeRecordedWith(readings -> {
                    final List<String> ids = readings.stream().map(TemperatureReading::sensorId).toList();
                    assertTrue(ids.contains("badezimmer_temperature"));
                    assertTrue(ids.contains("flur_temperature"));
                    assertTrue(ids.contains("kueche_temperature"));
                    assertTrue(ids.contains("schlafzimmer_temperature"));
                    assertTrue(ids.contains("wohnzimmer_temperature"));
                    readings.forEach(r -> {
                        assertEquals("indoor", r.location());
                        assertNotNull(r.humidityPct());
                    });
                })
                .verifyComplete();
    }

    private void stubOutdoor(List<FluxTable> temperatureTables, List<FluxTable> humidityTables) {
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenAnswer(invocation -> {
            final String query = invocation.getArgument(0);
            if (query.contains(TEST_HUMIDITY_ENTITY_ID)) {
                return humidityTables;
            }
            if (query.contains(TEST_ENTITY_ID)) {
                return temperatureTables;
            }
            return List.of();
        });
    }

    private FluxTable buildTable(double value, Instant time) {
        final FluxTable table = new FluxTable();
        final FluxRecord record = new FluxRecord(0);
        record.getValues().put("_value", value);
        record.getValues().put("_time", time);
        table.getRecords().add(record);
        return table;
    }
}
