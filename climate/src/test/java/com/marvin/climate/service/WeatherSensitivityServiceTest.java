package com.marvin.climate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.marvin.climate.biometeorology.BiometeorologyCalculator;
import com.marvin.climate.dto.ClimateTrend;
import com.marvin.climate.dto.WeatherSensitivity;
import com.marvin.climate.dto.WeatherSensitivity.Level;
import com.marvin.climate.dto.WeatherSensitivity.Trend;
import com.marvin.climate.weather.HourlyWeatherForecast;
import com.marvin.climate.weather.OpenWeatherMapClient;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherSensitivityService Tests")
class WeatherSensitivityServiceTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 10, 10, 12, 0);

    @Mock
    private ClimateTrendService climateTrendService;

    @Mock
    private OpenWeatherMapClient openWeatherMapClient;

    private WeatherSensitivityService service;

    @BeforeEach
    void setUp() {
        service = new WeatherSensitivityService(climateTrendService, openWeatherMapClient);
    }

    private static ClimateTrend trend(Double temp, Double dT3, Double dT24, Double dew, Double dDew3, Double pressure, Double dP3) {
        return new ClimateTrend(temp, dT3, dT24, dew, dDew3, pressure, dP3);
    }

    private static ClimateTrend quiet() {
        return trend(10.0, 0.0, 0.0, 5.0, 0.0, 1013.0, 0.0);
    }

    private static HourlyWeatherForecast forecast(int hoursAhead, double temp, Double pressure) {
        return new HourlyWeatherForecast(BASE_TIME.plusHours(hoursAhead), "10d", 500, "rain", temp, 60.0, 3.0, pressure, null, 52.5, 13.4);
    }

    private WeatherSensitivity evaluate(ClimateTrend trend, List<HourlyWeatherForecast> forecasts) {
        when(climateTrendService.getTrend()).thenReturn(Mono.just(trend));
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.fromIterable(forecasts));
        return service.getWeatherSensitivity().block();
    }

    private void assertLevel(Level expected, ClimateTrend trend) {
        final WeatherSensitivity result = evaluate(trend, List.of());
        assertEquals(expected, result.level());
    }

    // ---- no signals ----

    @Test
    @DisplayName("quiet weather yields KEINE with no reasons and STABLE trend")
    void quietWeather_isKeine() {
        final WeatherSensitivity result = evaluate(quiet(), List.of());
        assertEquals(Level.KEINE, result.level());
        assertEquals(Trend.STABLE, result.trend());
        assertTrue(result.reasons().isEmpty());
    }

    // ---- temperature drop 3 h ----

    @Test
    void temperatureDrop3h_minus5_isHoch_withKaltfrontReason() {
        final WeatherSensitivity result = evaluate(trend(10.0, -6.2, 0.0, 5.0, 0.0, 1013.0, 0.0), List.of());
        assertEquals(Level.HOCH, result.level());
        assertTrue(result.reasons().contains("Temperatursturz -6,2 K in 3 h (Kaltfront)"), result.reasons().toString());
    }

    @Test
    void temperatureDrop3h_exactly5_isHoch() {
        assertLevel(Level.HOCH, trend(10.0, -5.0, 0.0, 5.0, 0.0, 1013.0, 0.0));
    }

    @Test
    void temperatureDrop3h_between3And5_isGering() {
        assertLevel(Level.GERING, trend(10.0, -3.0, 0.0, 5.0, 0.0, 1013.0, 0.0));
        assertLevel(Level.GERING, trend(10.0, -4.9, 0.0, 5.0, 0.0, 1013.0, 0.0));
    }

    @Test
    void temperatureDrop3h_below3_isKeine() {
        assertLevel(Level.KEINE, trend(10.0, -2.9, 0.0, 5.0, 0.0, 1013.0, 0.0));
    }

    @Test
    void temperatureRise3h_isNoSignal() {
        assertLevel(Level.KEINE, trend(10.0, 8.0, 0.0, 5.0, 0.0, 1013.0, 0.0));
    }

    // ---- temperature drop 24 h ----

    @Test
    void temperatureDrop24h_thresholds() {
        assertLevel(Level.MITTEL, trend(10.0, 0.0, -8.0, 5.0, 0.0, 1013.0, 0.0));
        assertLevel(Level.GERING, trend(10.0, 0.0, -5.0, 5.0, 0.0, 1013.0, 0.0));
        assertLevel(Level.KEINE, trend(10.0, 0.0, -4.9, 5.0, 0.0, 1013.0, 0.0));
    }

    // ---- pressure drop ----

    @Test
    void pressureDrop_exactly3_isHoch() {
        final WeatherSensitivity result = evaluate(trend(10.0, 0.0, 0.0, 5.0, 0.0, 1010.0, -3.0), List.of());
        assertEquals(Level.HOCH, result.level());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("Luftdruckabfall -3,0 hPa in 3 h")), result.reasons().toString());
    }

    @Test
    void pressureDrop_between1And3_isMittel() {
        assertLevel(Level.MITTEL, trend(10.0, 0.0, 0.0, 5.0, 0.0, 1010.0, -1.0));
        assertLevel(Level.MITTEL, trend(10.0, 0.0, 0.0, 5.0, 0.0, 1010.0, -2.9));
    }

    @Test
    void pressureDrop_between05And1_isGering() {
        assertLevel(Level.GERING, trend(10.0, 0.0, 0.0, 5.0, 0.0, 1010.0, -0.5));
        assertLevel(Level.KEINE, trend(10.0, 0.0, 0.0, 5.0, 0.0, 1010.0, -0.4));
    }

    @Test
    void pressureRise_isNoSignal() {
        assertLevel(Level.KEINE, trend(10.0, 0.0, 0.0, 5.0, 0.0, 1020.0, 5.0));
    }

    // ---- dew point level ----

    @Test
    void dewPoint_20orMore_isHoch() {
        final WeatherSensitivity result = evaluate(trend(25.0, 0.0, 0.0, 20.0, 0.0, 1013.0, 0.0), List.of());
        assertEquals(Level.HOCH, result.level());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("Taupunkt 20,0 °C")), result.reasons().toString());
    }

    @Test
    void dewPoint_16to20_isMittel_withSchwuelReason() {
        final WeatherSensitivity result = evaluate(trend(22.0, 0.0, 0.0, 16.0, 0.0, 1013.0, 0.0), List.of());
        assertEquals(Level.MITTEL, result.level());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("Taupunkt 16,0 °C (schwül)")), result.reasons().toString());
        assertLevel(Level.MITTEL, trend(22.0, 0.0, 0.0, 19.9, 0.0, 1013.0, 0.0));
    }

    @Test
    void dewPoint_14to16_isGering() {
        assertLevel(Level.GERING, trend(20.0, 0.0, 0.0, 14.0, 0.0, 1013.0, 0.0));
        assertLevel(Level.GERING, trend(20.0, 0.0, 0.0, 15.9, 0.0, 1013.0, 0.0));
        assertLevel(Level.KEINE, trend(20.0, 0.0, 0.0, 13.9, 0.0, 1013.0, 0.0));
    }

    // ---- heat index ----

    @Test
    void heatIndex_between27And32_isMittel() {
        final double dew = BiometeorologyCalculator.dewPoint(30.0, 45.0);
        final WeatherSensitivity result = evaluate(trend(30.0, 0.0, 0.0, dew, 0.0, 1013.0, 0.0), List.of());
        assertNotNull(result.metrics().heatIndexOutside());
        assertTrue(result.metrics().heatIndexOutside() >= 27.0 && result.metrics().heatIndexOutside() < 32.0,
                String.valueOf(result.metrics().heatIndexOutside()));
        assertEquals(Level.MITTEL, result.level());
        assertTrue(result.reasons().stream().anyMatch(r -> r.startsWith("Hitzeindex")), result.reasons().toString());
    }

    @Test
    void heatIndex_32orMore_isHoch() {
        final double dew = BiometeorologyCalculator.dewPoint(33.0, 45.0);
        final WeatherSensitivity result = evaluate(trend(33.0, 0.0, 0.0, dew, 0.0, 1013.0, 0.0), List.of());
        assertTrue(result.metrics().heatIndexOutside() >= 32.0, String.valueOf(result.metrics().heatIndexOutside()));
        assertEquals(Level.HOCH, result.level());
        assertTrue(result.reasons().stream().anyMatch(r -> r.startsWith("Hitzeindex")), result.reasons().toString());
    }

    @Test
    void heatIndex_outsideValidityRange_isNullAndNoSignal() {
        final WeatherSensitivity result = evaluate(quiet(), List.of());
        assertNull(result.metrics().heatIndexOutside());
    }

    // ---- rising dew point ----

    @Test
    void dewPointRise3h_thresholds() {
        assertLevel(Level.MITTEL, trend(10.0, 0.0, 0.0, 5.0, 3.0, 1013.0, 0.0));
        assertLevel(Level.GERING, trend(10.0, 0.0, 0.0, 5.0, 2.0, 1013.0, 0.0));
        assertLevel(Level.KEINE, trend(10.0, 0.0, 0.0, 5.0, 1.9, 1013.0, 0.0));
        assertLevel(Level.KEINE, trend(10.0, 0.0, 0.0, 5.0, -4.0, 1013.0, 0.0));
    }

    // ---- maximum ----

    @Test
    void level_isMaximumOfSignals_andAllReasonsListed() {
        final WeatherSensitivity result = evaluate(trend(18.0, -6.0, -9.0, 17.0, 3.5, 1008.0, -2.0), List.of());
        assertEquals(Level.HOCH, result.level());
        assertEquals(5, result.reasons().size(), result.reasons().toString());
    }

    @Test
    void level_withOnlyLowSignals_isMaximumOfThem() {
        assertLevel(Level.MITTEL, trend(10.0, -3.5, -5.5, 5.0, 2.0, 1010.0, -1.5));
    }

    // ---- metrics ----

    @Test
    void metrics_areRoundedToOneDecimal_andExposeDerivedValues() {
        final WeatherSensitivity result = evaluate(trend(20.0, -6.24, -9.0, 17.06, 2.1, 1010.0, -2.4), List.of());
        assertEquals(17.1, result.metrics().dewPointOutside());
        assertEquals(-6.2, result.metrics().deltaT3h());
        assertEquals(-9.0, result.metrics().deltaT24h());
        assertEquals(2.1, result.metrics().deltaDewPoint3h());
        assertEquals(-2.4, result.metrics().deltaPressure3h());
        assertNotNull(result.metrics().absoluteHumidityOutside());
        assertTrue(result.metrics().absoluteHumidityOutside() > 5.0 && result.metrics().absoluteHumidityOutside() < 20.0);
    }

    // ---- forecast ----

    @Test
    void forecastTemperatureDrop_5orMore_isHoch() {
        final List<HourlyWeatherForecast> forecasts = List.of(forecast(3, 18.0, null), forecast(6, 16.0, null), forecast(9, 14.5, null));
        final WeatherSensitivity result = evaluate(trend(20.0, 0.0, 0.0, 5.0, 0.0, 1013.0, 0.0), forecasts);
        assertEquals(-5.5, result.metrics().forecastDeltaT());
        assertEquals(Level.HOCH, result.level());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("-5,5 K") && r.contains("Prognose")), result.reasons().toString());
    }

    @Test
    void forecastPressureDrop_isHoch() {
        final List<HourlyWeatherForecast> forecasts = List.of(forecast(3, 10.0, 1015.0), forecast(6, 10.0, 1013.0), forecast(9, 10.0, 1011.9));
        final WeatherSensitivity result = evaluate(quiet(), forecasts);
        assertEquals(-3.1, result.metrics().forecastDeltaPressure());
        assertEquals(Level.HOCH, result.level());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("-3,1 hPa") && r.contains("Prognose")), result.reasons().toString());
    }

    @Test
    void forecastPressureDrop_between1And3_isMittel() {
        final List<HourlyWeatherForecast> forecasts = List.of(forecast(3, 10.0, 1015.0), forecast(9, 10.0, 1013.0));
        assertEquals(Level.MITTEL, evaluate(quiet(), forecasts).level());
    }

    @Test
    void forecastWithoutPressure_leavesForecastPressureNull() {
        final List<HourlyWeatherForecast> forecasts = List.of(forecast(3, 10.0, null), forecast(9, 10.0, null));
        final WeatherSensitivity result = evaluate(quiet(), forecasts);
        assertNull(result.metrics().forecastDeltaPressure());
        assertEquals(Level.KEINE, result.level());
    }

    // ---- trend ----

    @Test
    void trend_isRising_whenForecastSignalStrongerThanMeasured() {
        final List<HourlyWeatherForecast> forecasts = List.of(forecast(3, 9.0, null), forecast(9, 4.0, null));
        assertEquals(Trend.RISING, evaluate(quiet(), forecasts).trend());
    }

    @Test
    void trend_isFalling_whenForecastSignalWeakerThanMeasured() {
        final List<HourlyWeatherForecast> forecasts = List.of(forecast(3, 10.0, 1010.0), forecast(9, 10.0, 1010.0));
        assertEquals(Trend.FALLING, evaluate(trend(10.0, -6.0, 0.0, 5.0, 0.0, 1013.0, 0.0), forecasts).trend());
    }

    @Test
    void trend_isStable_whenForecastSignalEqualsMeasured() {
        final List<HourlyWeatherForecast> forecasts = List.of(forecast(3, 10.0, 1010.0), forecast(9, 10.0, 1010.0));
        assertEquals(Trend.STABLE, evaluate(quiet(), forecasts).trend());
    }

    @Test
    void trend_isStable_whenNoForecastAvailable() {
        assertEquals(Trend.STABLE, evaluate(trend(10.0, -6.0, 0.0, 5.0, 0.0, 1013.0, 0.0), List.of()).trend());
    }

    // ---- missing data ----

    @Test
    void missingTrendValues_yieldKeineWithNullMetrics() {
        final WeatherSensitivity result = evaluate(trend(null, null, null, null, null, null, null), List.of());
        assertEquals(Level.KEINE, result.level());
        assertNull(result.metrics().dewPointOutside());
        assertNull(result.metrics().absoluteHumidityOutside());
        assertNull(result.metrics().heatIndexOutside());
        assertNull(result.metrics().deltaT3h());
        assertNull(result.metrics().forecastDeltaT());
        assertNull(result.metrics().forecastDeltaPressure());
    }

    @Test
    void missingPressure_stillEvaluatesTemperatureAndDewPoint() {
        final WeatherSensitivity result = evaluate(trend(10.0, -5.5, -2.0, 5.0, 0.0, null, null), List.of());
        assertEquals(Level.HOCH, result.level());
        assertNull(result.metrics().deltaPressure3h());
    }

    @Test
    void owmError_doesNotFail_andUsesMeasuredDataOnly() {
        when(climateTrendService.getTrend()).thenReturn(Mono.just(trend(10.0, -6.0, 0.0, 5.0, 0.0, 1013.0, 0.0)));
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.error(new IllegalStateException("OWM down")));

        StepVerifier.create(service.getWeatherSensitivity())
                .assertNext(result -> {
                    assertEquals(Level.HOCH, result.level());
                    assertEquals(Trend.STABLE, result.trend());
                    assertNull(result.metrics().forecastDeltaT());
                })
                .verifyComplete();
    }

    @Test
    void trendServiceError_doesNotFail_andYieldsKeine() {
        when(climateTrendService.getTrend()).thenReturn(Mono.error(new IllegalStateException("influx down")));
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.empty());

        StepVerifier.create(service.getWeatherSensitivity())
                .assertNext(result -> assertEquals(Level.KEINE, result.level()))
                .verifyComplete();
    }

    @Test
    void emptyTrendMono_doesNotFail() {
        when(climateTrendService.getTrend()).thenReturn(Mono.empty());
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.empty());

        StepVerifier.create(service.getWeatherSensitivity())
                .assertNext(result -> assertEquals(Level.KEINE, result.level()))
                .verifyComplete();
    }
}
