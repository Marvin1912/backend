package com.marvin.plants.service;

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
import java.time.temporal.ChronoUnit;
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
@DisplayName("PlantMoistureService Tests")
class PlantMoistureServiceTest {

    private static final String TEST_ORG = "test_org";
    private static final String TEST_ENTITY_ID = "feder_calathea_soil_moisture";
    private static final String TEST_PLANT_NAME = "Feder Calathea";
    private static final double TEST_MOISTURE = 42.5;

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private QueryApi queryApi;

    private PlantMoistureService plantMoistureService;

    @BeforeEach
    void setUp() {
        plantMoistureService = new PlantMoistureService(influxDBClient, TEST_ORG);
    }

    @Test
    @DisplayName("Should emit moisture reading when InfluxDB returns a record")
    void getCurrentMoisture_ShouldEmitReading_WhenDataPresent() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG)))
                .thenReturn(List.of(buildTable(TEST_MOISTURE, measuredAt)));

        // When / Then
        StepVerifier.create(plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, TEST_ENTITY_ID))
                .assertNext(reading -> {
                    assertEquals(TEST_PLANT_NAME, reading.plantName());
                    assertEquals(TEST_MOISTURE, reading.moisturePercent());
                    assertEquals(measuredAt, reading.measuredAt());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should build a Flux query filtering on the sensor_data bucket, '%' measurement and the given entity_id")
    void getCurrentMoisture_ShouldQueryWithExpectedFluxStatement() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());
        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

        // When
        plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, TEST_ENTITY_ID).block();

        // Then
        verify(queryApi).query(queryCaptor.capture(), eq(TEST_ORG));
        final String query = queryCaptor.getValue();
        assertEquals(true, query.contains("sensor_data"));
        assertEquals(true, query.contains("r._measurement == \"%\""));
        assertEquals(true, query.contains(TEST_ENTITY_ID));
        assertEquals(true, query.contains("r._field == \"value\""));
    }

    @Test
    @DisplayName("Should query with a 30 day range instead of 24h, since state-change-based sensor "
            + "logging can leave soil moisture values unchanged (and thus unwritten) for days")
    void getCurrentMoisture_ShouldQueryWithExpandedRange_ToToleratesStaleButValidReadings() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());
        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

        // When
        plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, TEST_ENTITY_ID).block();

        // Then
        verify(queryApi).query(queryCaptor.capture(), eq(TEST_ORG));
        final String query = queryCaptor.getValue();
        assertEquals(true, query.contains("range(start: -30d)"));
        assertEquals(false, query.contains("-24h"));
    }

    @Test
    @DisplayName("Should return the last known reading even when its timestamp is older than 24h, "
            + "since Home Assistant only logs a new point when the sensor value actually changes")
    void getCurrentMoisture_ShouldReturnStaleReading_WhenSensorHasNotChangedRecently() {
        // Given
        final Instant measuredAt = Instant.now().minus(10, ChronoUnit.DAYS);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG)))
                .thenReturn(List.of(buildTable(TEST_MOISTURE, measuredAt)));

        // When / Then
        StepVerifier.create(plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, TEST_ENTITY_ID))
                .assertNext(reading -> {
                    assertEquals(TEST_PLANT_NAME, reading.plantName());
                    assertEquals(TEST_MOISTURE, reading.moisturePercent());
                    assertEquals(measuredAt, reading.measuredAt());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty Mono when InfluxDB returns no records")
    void getCurrentMoisture_ShouldReturnEmpty_WhenNoDataAvailable() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());

        // When / Then
        StepVerifier.create(plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, TEST_ENTITY_ID))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty Mono when the InfluxDB query fails")
    void getCurrentMoisture_ShouldReturnEmpty_WhenQueryFails() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenThrow(new RuntimeException("influx down"));

        // When / Then
        StepVerifier.create(plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, TEST_ENTITY_ID))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty Mono without querying InfluxDB when entity id is null")
    void getCurrentMoisture_ShouldReturnEmpty_WhenEntityIdNull() {
        // When / Then
        StepVerifier.create(plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, null))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty Mono without querying InfluxDB when entity id is blank")
    void getCurrentMoisture_ShouldReturnEmpty_WhenEntityIdBlank() {
        // When / Then
        StepVerifier.create(plantMoistureService.getCurrentMoisture(TEST_PLANT_NAME, "   "))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return sorted distinct entity ids when InfluxDB returns moisture sensor records")
    void listAvailableSensorEntityIds_ShouldReturnSortedDistinctIds_WhenDataPresent() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of(
                buildEntityIdTable("zebra_plant_soil_moisture"),
                buildEntityIdTable("alpha_plant_soil_moisture"),
                buildEntityIdTable("alpha_plant_soil_moisture")
        ));

        // When / Then
        StepVerifier.create(plantMoistureService.listAvailableSensorEntityIds())
                .assertNext(ids -> assertEquals(
                        List.of("alpha_plant_soil_moisture", "zebra_plant_soil_moisture"), ids))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should build a Flux query filtering on the sensor_data bucket, '%' measurement and entity_id matching 'moisture'")
    void listAvailableSensorEntityIds_ShouldQueryWithExpectedFluxStatement() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());
        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

        // When
        plantMoistureService.listAvailableSensorEntityIds().block();

        // Then
        verify(queryApi).query(queryCaptor.capture(), eq(TEST_ORG));
        final String query = queryCaptor.getValue();
        assertEquals(true, query.contains("sensor_data"));
        assertEquals(true, query.contains("r._measurement == \"%\""));
        assertEquals(true, query.contains("r._field == \"value\""));
        assertEquals(true, query.contains("r.entity_id =~ /moisture/"));
        assertEquals(true, query.contains("distinct(column: \"entity_id\")"));
    }

    @Test
    @DisplayName("Should return empty list when InfluxDB returns no moisture sensor records")
    void listAvailableSensorEntityIds_ShouldReturnEmptyList_WhenNoDataAvailable() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenReturn(List.of());

        // When / Then
        StepVerifier.create(plantMoistureService.listAvailableSensorEntityIds())
                .assertNext(ids -> assertTrue(ids.isEmpty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when the InfluxDB query fails")
    void listAvailableSensorEntityIds_ShouldReturnEmptyList_WhenQueryFails() {
        // Given
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenThrow(new RuntimeException("influx down"));

        // When / Then
        StepVerifier.create(plantMoistureService.listAvailableSensorEntityIds())
                .assertNext(ids -> assertTrue(ids.isEmpty()))
                .verifyComplete();
    }

    private FluxTable buildTable(double value, Instant time) {
        final FluxTable table = new FluxTable();
        final FluxRecord record = new FluxRecord(0);
        record.getValues().put("_value", value);
        record.getValues().put("_time", time);
        table.getRecords().add(record);
        return table;
    }

    private FluxTable buildEntityIdTable(String entityId) {
        final FluxTable table = new FluxTable();
        final FluxRecord record = new FluxRecord(0);
        record.getValues().put("_value", entityId);
        table.getRecords().add(record);
        return table;
    }
}
