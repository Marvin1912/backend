package com.marvin.climate.controller;

import static org.mockito.Mockito.when;

import com.marvin.climate.dto.TemperatureReading;
import com.marvin.climate.service.ClimateService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest(
        controllers = ClimateController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@DisplayName("ClimateController Tests")
class ClimateControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ClimateService climateService;

    @Test
    @DisplayName("GET /climate/readings returns 200 with a JSON array of readings")
    void getReadings_ShouldReturn200WithReadings_WhenServiceHasData() {
        // Given
        final Instant measuredAt = Instant.parse("2026-05-16T10:00:00Z");
        final TemperatureReading reading = new TemperatureReading(
                "draussen_temperature",
                "Draußen",
                "outdoor",
                21.5,
                measuredAt
        );
        when(climateService.getCurrentReadings()).thenReturn(Flux.just(reading));

        // When / Then
        webTestClient.get()
                .uri("/climate/readings")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].sensorId").isEqualTo("draussen_temperature")
                .jsonPath("$[0].label").isEqualTo("Draußen")
                .jsonPath("$[0].location").isEqualTo("outdoor")
                .jsonPath("$[0].temperatureC").isEqualTo(21.5)
                .jsonPath("$[0].measuredAt").isEqualTo("2026-05-16T10:00:00Z");
    }

    @Test
    @DisplayName("GET /climate/readings returns 200 with empty array when no data")
    void getReadings_ShouldReturn200WithEmptyArray_WhenNoData() {
        // Given
        when(climateService.getCurrentReadings()).thenReturn(Flux.empty());

        // When / Then
        webTestClient.get()
                .uri("/climate/readings")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
