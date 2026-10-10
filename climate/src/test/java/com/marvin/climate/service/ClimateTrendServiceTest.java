package com.marvin.climate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.marvin.climate.biometeorology.BiometeorologyCalculator;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClimateTrendService Tests")
class ClimateTrendServiceTest {

    private static final String TEST_ORG = "test_org";
    private static final String TEMPERATURE_ID = "draussen_temperature";
    private static final String HUMIDITY_ID = "draussen_humidity";
    private static final List<String> PRESSURE_IDS = List.of(
            "wohnzimmer_pressure", "flur_pressure", "schlafzimmer_pressure", "kueche_pressure", "badezimmer_pressure");
    private static final double DELTA = 1e-9;

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private QueryApi queryApi;

    private ClimateTrendService service;

    /** Values keyed by "entityId@point" where point is one of now, 3h, 24h. */
    private final Map<String, Double> data = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new ClimateTrendService(influxDBClient, TEST_ORG, TEMPERATURE_ID, HUMIDITY_ID);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        lenient().when(queryApi.query(anyString(), eq(TEST_ORG))).thenAnswer(invocation -> {
            final String query = invocation.getArgument(0);
            final String point = pointOf(query);
            for (final Map.Entry<String, Double> entry : data.entrySet()) {
                final String[] parts = entry.getKey().split("@");
                if (query.contains("\"" + parts[0] + "\"") && parts[1].equals(point)) {
                    return List.of(buildTable(entry.getValue()));
                }
            }
            return List.of();
        });
    }

    @Test
    @DisplayName("Should compute temperature deltas against 3 h and 24 h ago")
    void getTrend_ShouldComputeTemperatureDeltas() {
        data.put(TEMPERATURE_ID + "@now", 20.0);
        data.put(TEMPERATURE_ID + "@3h", 17.5);
        data.put(TEMPERATURE_ID + "@24h", 22.0);

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertEquals(20.0, trend.temperatureC(), DELTA);
                    assertEquals(2.5, trend.temperatureDelta3h(), DELTA);
                    assertEquals(-2.0, trend.temperatureDelta24h(), DELTA);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should leave deltas empty when the historic temperature is missing")
    void getTrend_ShouldLeaveDeltasEmpty_WhenHistoricTemperatureMissing() {
        data.put(TEMPERATURE_ID + "@now", 20.0);
        data.put(TEMPERATURE_ID + "@24h", 22.0);

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertEquals(20.0, trend.temperatureC(), DELTA);
                    assertNull(trend.temperatureDelta3h());
                    assertEquals(-2.0, trend.temperatureDelta24h(), DELTA);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should compute dew point and its 3 h delta from temperature and humidity")
    void getTrend_ShouldComputeDewPointDelta() {
        data.put(TEMPERATURE_ID + "@now", 20.0);
        data.put(HUMIDITY_ID + "@now", 60.0);
        data.put(TEMPERATURE_ID + "@3h", 15.0);
        data.put(HUMIDITY_ID + "@3h", 80.0);

        final double dewNow = BiometeorologyCalculator.dewPoint(20.0, 60.0);
        final double dewBefore = BiometeorologyCalculator.dewPoint(15.0, 80.0);

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertEquals(dewNow, trend.dewPointC(), DELTA);
                    assertEquals(dewNow - dewBefore, trend.dewPointDelta3h(), DELTA);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should leave dew point empty when humidity is missing")
    void getTrend_ShouldLeaveDewPointEmpty_WhenHumidityMissing() {
        data.put(TEMPERATURE_ID + "@now", 20.0);
        data.put(TEMPERATURE_ID + "@3h", 15.0);
        data.put(HUMIDITY_ID + "@3h", 80.0);

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertNull(trend.dewPointC());
                    assertNull(trend.dewPointDelta3h());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should leave dew point empty when humidity is out of range")
    void getTrend_ShouldLeaveDewPointEmpty_WhenHumidityInvalid() {
        data.put(TEMPERATURE_ID + "@now", 20.0);
        data.put(HUMIDITY_ID + "@now", 0.0);

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> assertNull(trend.dewPointC()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should use the median of an odd number of pressure sensors")
    void getTrend_ShouldUseMedianPressure_WhenOddCount() {
        final double[] now = {1010.0, 1012.0, 1000.0, 1011.0, 1013.0};
        final double[] before = {1005.0, 1006.0, 1007.0, 1008.0, 1009.0};
        for (int i = 0; i < PRESSURE_IDS.size(); i++) {
            data.put(PRESSURE_IDS.get(i) + "@now", now[i]);
            data.put(PRESSURE_IDS.get(i) + "@3h", before[i]);
        }

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertEquals(1011.0, trend.pressureHpa(), DELTA);
                    assertEquals(4.0, trend.pressureDelta3h(), DELTA);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should average the two middle values for an even number of pressure sensors")
    void getTrend_ShouldUseMedianPressure_WhenEvenCount() {
        data.put("wohnzimmer_pressure@now", 1000.0);
        data.put("flur_pressure@now", 1002.0);
        data.put("kueche_pressure@now", 1010.0);
        data.put("badezimmer_pressure@now", 1004.0);

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertEquals(1003.0, trend.pressureHpa(), DELTA);
                    assertNull(trend.pressureDelta3h());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should report pressure as unavailable instead of zero when Influx has none")
    void getTrend_ShouldLeavePressureEmpty_WhenNoPressureData() {
        data.put(TEMPERATURE_ID + "@now", 20.0);

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertNotNull(trend);
                    assertNull(trend.pressureHpa());
                    assertNull(trend.pressureDelta3h());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit an all-empty trend instead of failing when queries fail")
    void getTrend_ShouldEmitEmptyTrend_WhenQueriesFail() {
        when(queryApi.query(anyString(), eq(TEST_ORG))).thenThrow(new RuntimeException("influx down"));

        StepVerifier.create(service.getTrend())
                .assertNext(trend -> {
                    assertNull(trend.temperatureC());
                    assertNull(trend.temperatureDelta3h());
                    assertNull(trend.temperatureDelta24h());
                    assertNull(trend.dewPointC());
                    assertNull(trend.dewPointDelta3h());
                    assertNull(trend.pressureHpa());
                    assertNull(trend.pressureDelta3h());
                })
                .verifyComplete();
    }

    private static String pointOf(String query) {
        if (query.contains("stop: -180m")) {
            return "3h";
        }
        if (query.contains("stop: -1440m")) {
            return "24h";
        }
        return "now";
    }

    private FluxTable buildTable(double value) {
        final FluxTable table = new FluxTable();
        final FluxRecord record = new FluxRecord(0);
        record.getValues().put("_value", value);
        record.getValues().put("_time", Instant.parse("2026-05-16T10:00:00Z"));
        table.getRecords().add(record);
        return table;
    }
}
