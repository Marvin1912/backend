package com.marvin.climate.controller;

import static org.mockito.Mockito.when;

import com.marvin.climate.dto.WeatherSensitivity;
import com.marvin.climate.dto.WeatherSensitivity.Level;
import com.marvin.climate.dto.WeatherSensitivity.Metrics;
import com.marvin.climate.dto.WeatherSensitivity.Trend;
import com.marvin.climate.service.WeatherSensitivityService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(
        controllers = WeatherSensitivityController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@DisplayName("WeatherSensitivityController Tests")
class WeatherSensitivityControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private WeatherSensitivityService weatherSensitivityService;

    @Test
    @DisplayName("GET /climate/weather-sensitivity returns level, trend, reasons and metrics")
    void getWeatherSensitivity_ShouldReturnAssessment() {
        // Given
        final Metrics metrics = new Metrics(17.1, 9.8, null, -6.2, -9.0, 2.1, -2.4, -5.5, -3.1);
        final WeatherSensitivity assessment = new WeatherSensitivity(
                Level.HOCH, Trend.RISING, List.of("Temperatursturz -6,2 K in 3 h (Kaltfront)"), metrics);
        when(weatherSensitivityService.getWeatherSensitivity()).thenReturn(Mono.just(assessment));

        // When / Then
        webTestClient.get()
                .uri("/climate/weather-sensitivity")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.level").isEqualTo("HOCH")
                .jsonPath("$.trend").isEqualTo("RISING")
                .jsonPath("$.reasons[0]").isEqualTo("Temperatursturz -6,2 K in 3 h (Kaltfront)")
                .jsonPath("$.metrics.dewPointOutside").isEqualTo(17.1)
                .jsonPath("$.metrics.absoluteHumidityOutside").isEqualTo(9.8)
                .jsonPath("$.metrics.heatIndexOutside").isEmpty()
                .jsonPath("$.metrics.deltaT3h").isEqualTo(-6.2)
                .jsonPath("$.metrics.deltaT24h").isEqualTo(-9.0)
                .jsonPath("$.metrics.deltaDewPoint3h").isEqualTo(2.1)
                .jsonPath("$.metrics.deltaPressure3h").isEqualTo(-2.4)
                .jsonPath("$.metrics.forecastDeltaT").isEqualTo(-5.5)
                .jsonPath("$.metrics.forecastDeltaPressure").isEqualTo(-3.1);
    }

    @Test
    @DisplayName("GET /climate/weather-sensitivity returns 200 with KEINE and empty reasons when nothing is triggered")
    void getWeatherSensitivity_ShouldReturnKeine_WhenNoSignals() {
        // Given
        final Metrics metrics = new Metrics(null, null, null, null, null, null, null, null, null);
        when(weatherSensitivityService.getWeatherSensitivity())
                .thenReturn(Mono.just(new WeatherSensitivity(Level.KEINE, Trend.STABLE, List.of(), metrics)));

        // When / Then
        webTestClient.get()
                .uri("/climate/weather-sensitivity")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.level").isEqualTo("KEINE")
                .jsonPath("$.trend").isEqualTo("STABLE")
                .jsonPath("$.reasons").isEmpty();
    }
}
