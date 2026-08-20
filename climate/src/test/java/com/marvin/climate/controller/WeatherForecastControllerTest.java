package com.marvin.climate.controller;

import static org.mockito.Mockito.when;

import com.marvin.climate.weather.HourlyWeatherForecast;
import com.marvin.climate.weather.OpenWeatherMapClient;
import com.marvin.climate.weather.WeatherForecast;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest(
        controllers = WeatherForecastController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@DisplayName("WeatherForecastController Tests")
class WeatherForecastControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OpenWeatherMapClient openWeatherMapClient;

    @Test
    @DisplayName("GET /climate/forecast returns forecast entries including icon code and weather id")
    void getForecast_ShouldReturnForecastEntries() {
        // Given
        final WeatherForecast forecast = new WeatherForecast(
                LocalDate.of(2026, 8, 17),
                "10d",
                500,
                "light rain",
                21.5,
                60.0,
                3.5,
                52.52,
                13.405
        );
        when(openWeatherMapClient.getForecast()).thenReturn(Flux.just(forecast));

        // When / Then
        webTestClient.get()
                .uri("/climate/forecast")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].date").isEqualTo("2026-08-17")
                .jsonPath("$[0].iconCode").isEqualTo("10d")
                .jsonPath("$[0].weatherId").isEqualTo(500)
                .jsonPath("$[0].description").isEqualTo("light rain")
                .jsonPath("$[0].temperatureC").isEqualTo(21.5)
                .jsonPath("$[0].humidityPct").isEqualTo(60.0)
                .jsonPath("$[0].windSpeedMs").isEqualTo(3.5)
                .jsonPath("$[0].latitude").isEqualTo(52.52)
                .jsonPath("$[0].longitude").isEqualTo(13.405);
    }

    @Test
    @DisplayName("GET /climate/forecast returns 200 with empty array when no forecast data is available")
    void getForecast_ShouldReturn200WithEmptyArray_WhenNoData() {
        // Given
        when(openWeatherMapClient.getForecast()).thenReturn(Flux.empty());

        // When / Then
        webTestClient.get()
                .uri("/climate/forecast")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /climate/forecast/hourly returns hourly forecast entries including icon code and weather id")
    void getHourlyForecast_ShouldReturnHourlyForecastEntries() {
        // Given
        final HourlyWeatherForecast forecast = new HourlyWeatherForecast(
                LocalDateTime.of(2026, 8, 16, 9, 0),
                "10d",
                500,
                "light rain",
                21.5,
                60.0,
                3.5,
                52.52,
                13.405
        );
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.just(forecast));

        // When / Then
        webTestClient.get()
                .uri("/climate/forecast/hourly")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].dateTime").isEqualTo("2026-08-16T09:00:00")
                .jsonPath("$[0].iconCode").isEqualTo("10d")
                .jsonPath("$[0].weatherId").isEqualTo(500)
                .jsonPath("$[0].description").isEqualTo("light rain")
                .jsonPath("$[0].temperatureC").isEqualTo(21.5)
                .jsonPath("$[0].humidityPct").isEqualTo(60.0)
                .jsonPath("$[0].windSpeedMs").isEqualTo(3.5)
                .jsonPath("$[0].latitude").isEqualTo(52.52)
                .jsonPath("$[0].longitude").isEqualTo(13.405);
    }

    @Test
    @DisplayName("GET /climate/forecast/hourly returns 200 with empty array when no forecast data is available")
    void getHourlyForecast_ShouldReturn200WithEmptyArray_WhenNoData() {
        // Given
        when(openWeatherMapClient.getHourlyForecast()).thenReturn(Flux.empty());

        // When / Then
        webTestClient.get()
                .uri("/climate/forecast/hourly")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
