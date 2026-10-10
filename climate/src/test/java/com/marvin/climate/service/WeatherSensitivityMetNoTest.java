package com.marvin.climate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.climate.dto.ClimateTrend;
import com.marvin.climate.dto.WeatherSensitivity;
import com.marvin.climate.dto.WeatherSensitivity.Level;
import com.marvin.climate.weather.HourlyWeatherForecast;
import com.marvin.climate.weather.MetNoForecastClient;
import com.marvin.climate.weather.OpenWeatherMapClient;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Tests for the met.no primary forecast with OpenWeatherMap fallback in {@link WeatherSensitivityService}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherSensitivityService met.no Tests")
class WeatherSensitivityMetNoTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 10, 10, 12, 0);

    @Mock
    private ClimateTrendService climateTrendService;

    @Mock
    private OpenWeatherMapClient openWeatherMapClient;

    @Mock
    private MetNoForecastClient metNoForecastClient;

    private WeatherSensitivityService service;

    @BeforeEach
    void setUp() {
        service = new WeatherSensitivityService(climateTrendService, openWeatherMapClient, metNoForecastClient);
        when(climateTrendService.getTrend()).thenReturn(Mono.just(new ClimateTrend(10.0, 0.0, 0.0, 5.0, 0.0, 1013.0, 0.0)));
    }

    private static HourlyWeatherForecast forecast(final int hoursAhead, final double temp, final Double pressure, final Double dewPoint) {
        return new HourlyWeatherForecast(BASE_TIME.plusHours(hoursAhead), null, 0, null, temp, 60.0, 3.0, pressure, null, dewPoint, 52.5, 13.4);
    }

    @Test
    @DisplayName("uses met.no as the primary forecast without calling OpenWeatherMap")
    void usesMetNo_whenAvailable() {
        when(metNoForecastClient.getHourlyForecast()).thenReturn(Flux.just(
                forecast(1, 9.0, 1010.0, null), forecast(4, 3.0, 1005.0, null)));

        final WeatherSensitivity result = service.getWeatherSensitivity().block();

        assertEquals(Level.HOCH, result.level());
        assertEquals(-7.0, result.metrics().forecastDeltaT());
        assertEquals(-5.0, result.metrics().forecastDeltaPressure());
        verify(openWeatherMapClient, never()).getHourlyForecast();
    }

    @Test
    @DisplayName("falls back to OpenWeatherMap when met.no returns nothing")
    void fallsBack_whenMetNoEmpty() {
        when(metNoForecastClient.getHourlyForecast()).thenReturn(Flux.empty());
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.just(
                forecast(3, 9.0, 1010.0, null), forecast(6, 3.0, 1005.0, null)));

        final WeatherSensitivity result = service.getWeatherSensitivity().block();

        assertEquals(-7.0, result.metrics().forecastDeltaT());
    }

    @Test
    @DisplayName("falls back to OpenWeatherMap when met.no fails")
    void fallsBack_whenMetNoFails() {
        when(metNoForecastClient.getHourlyForecast()).thenReturn(Flux.error(new IllegalStateException("down")));
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.just(
                forecast(3, 4.0, 1010.0, null), forecast(6, 3.0, 1009.0, null)));

        final WeatherSensitivity result = service.getWeatherSensitivity().block();

        assertEquals(-7.0, result.metrics().forecastDeltaT());
    }

    @Test
    @DisplayName("does not error and yields no forecast signals when both sources fail")
    void noError_whenBothFail() {
        when(metNoForecastClient.getHourlyForecast()).thenReturn(Flux.error(new IllegalStateException("down")));
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.error(new IllegalStateException("down")));

        final WeatherSensitivity result = service.getWeatherSensitivity().block();

        assertEquals(Level.KEINE, result.level());
    }

    @Test
    @DisplayName("forecast dew point rise triggers a signal relative to the current dew point")
    void forecastDewPointRise_triggersSignal() {
        when(metNoForecastClient.getHourlyForecast()).thenReturn(Flux.just(
                forecast(1, 10.0, 1013.0, 6.0), forecast(4, 10.0, 1013.0, 7.5)));

        final WeatherSensitivity result = service.getWeatherSensitivity().block();

        assertEquals(Level.GERING, result.level());
        assertTrue(result.reasons().contains("Taupunkt steigt schnell: +2,5 K laut Prognose"), result.reasons().toString());
    }

    @Test
    @DisplayName("forecast dew point rise below 2 K is no signal")
    void smallForecastDewPointRise_noSignal() {
        when(metNoForecastClient.getHourlyForecast()).thenReturn(Flux.just(
                forecast(1, 10.0, 1013.0, 6.0), forecast(4, 10.0, 1013.0, 6.9)));

        assertEquals(Level.KEINE, service.getWeatherSensitivity().block().level());
    }

    @Test
    @DisplayName("dew point rise of 3 K is MITTEL")
    void forecastDewPointRise_mittel() {
        when(metNoForecastClient.getHourlyForecast()).thenReturn(Flux.just(
                forecast(1, 10.0, 1013.0, 6.0), forecast(4, 10.0, 1013.0, 8.0)));

        assertEquals(Level.MITTEL, service.getWeatherSensitivity().block().level());
    }

    @Test
    @DisplayName("keeps working with the two-argument constructor using OpenWeatherMap only")
    void twoArgConstructor_usesOpenWeatherMapOnly() {
        final WeatherSensitivityService legacy = new WeatherSensitivityService(climateTrendService, openWeatherMapClient);
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.just(forecast(3, 9.0, null, null)));

        assertEquals(-1.0, legacy.getWeatherSensitivity().block().metrics().forecastDeltaT());
    }
}
