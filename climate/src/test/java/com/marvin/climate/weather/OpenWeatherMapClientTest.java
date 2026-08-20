package com.marvin.climate.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link OpenWeatherMapClient} covering aggregation, mapping, and edge cases. */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenWeatherMapClientTest")
class OpenWeatherMapClientTest {

    private static final String API_KEY = "test-api-key";
    private static final double LAT = 52.52;
    private static final double LON = 13.405;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private OpenWeatherMapClient openWeatherMapClient;

    /** Sets up the WebClient mock chain and creates the client under test. */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.baseUrl(any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        openWeatherMapClient = new OpenWeatherMapClient(webClientBuilder, API_KEY, LAT, LON);
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientChain(Mono<?> responseMono) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(String.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseMono).when(responseSpec).bodyToMono(any(Class.class));
    }

    private OpenWeatherMapClient.WeatherInfo weather(int id, String description, String icon) {
        return new OpenWeatherMapClient.WeatherInfo(id, description, icon);
    }

    private OpenWeatherMapClient.MainInfo main(double temp, int humidity) {
        return new OpenWeatherMapClient.MainInfo(temp, humidity);
    }

    private OpenWeatherMapClient.WindInfo wind(double speed) {
        return new OpenWeatherMapClient.WindInfo(speed);
    }

    private OpenWeatherMapClient.ForecastEntry entry(String dtTxt, double temp, int humidity, double windSpeed,
            int weatherId, String description, String icon) {
        return new OpenWeatherMapClient.ForecastEntry(
                dtTxt,
                main(temp, humidity),
                List.of(weather(weatherId, description, icon)),
                wind(windSpeed));
    }

    @Test
    @DisplayName("Should pick the entry closest to midday for each day and map all fields")
    void getForecast_ShouldPickMiddayEntry_AndMapFields() {
        // Given
        final OpenWeatherMapClient.ForecastResponse response = new OpenWeatherMapClient.ForecastResponse(List.of(
                entry("2026-08-16 09:00:00", 18.0, 70, 2.0, 800, "clear sky", "01d"),
                entry("2026-08-16 12:00:00", 22.5, 60, 3.5, 500, "light rain", "10d"),
                entry("2026-08-16 15:00:00", 21.0, 65, 3.0, 500, "light rain", "10d")
        ));
        stubWebClientChain(Mono.just(response));

        // When / Then
        StepVerifier.create(openWeatherMapClient.getForecast())
                .assertNext(forecast -> {
                    assertEquals(LocalDate.of(2026, 8, 16), forecast.date());
                    assertEquals("10d", forecast.iconCode());
                    assertEquals(500, forecast.weatherId());
                    assertEquals("light rain", forecast.description());
                    assertEquals(22.5, forecast.temperatureC());
                    assertEquals(60.0, forecast.humidityPct());
                    assertEquals(3.5, forecast.windSpeedMs());
                    assertEquals(LAT, forecast.latitude());
                    assertEquals(LON, forecast.longitude());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return at most three days, sorted ascending by date")
    void getForecast_ShouldLimitToThreeDays_SortedAscending() {
        // Given
        final OpenWeatherMapClient.ForecastResponse response = new OpenWeatherMapClient.ForecastResponse(List.of(
                entry("2026-08-19 12:00:00", 25.0, 50, 1.0, 800, "clear sky", "01d"),
                entry("2026-08-16 12:00:00", 20.0, 55, 1.5, 800, "clear sky", "01d"),
                entry("2026-08-17 12:00:00", 21.0, 55, 1.5, 800, "clear sky", "01d"),
                entry("2026-08-18 12:00:00", 22.0, 55, 1.5, 800, "clear sky", "01d")
        ));
        stubWebClientChain(Mono.just(response));

        // When / Then
        StepVerifier.create(openWeatherMapClient.getForecast())
                .recordWith(java.util.ArrayList::new)
                .expectNextCount(3)
                .consumeRecordedWith(forecasts -> {
                    final List<LocalDate> dates = forecasts.stream().map(WeatherForecast::date).toList();
                    assertEquals(List.of(
                            LocalDate.of(2026, 8, 16),
                            LocalDate.of(2026, 8, 17),
                            LocalDate.of(2026, 8, 18)
                    ), dates);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit an empty Flux when the API returns no forecast entries")
    void getForecast_ShouldReturnEmpty_WhenListIsEmpty() {
        // Given
        stubWebClientChain(Mono.just(new OpenWeatherMapClient.ForecastResponse(List.of())));

        // When / Then
        StepVerifier.create(openWeatherMapClient.getForecast())
                .verifyComplete();
    }

    private Clock fixedClockAt(LocalDateTime dateTime) {
        return Clock.fixed(dateTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Should return the next three upcoming entries relative to now")
    void getHourlyForecast_ShouldReturnNextThreeEntries_RelativeToNow() {
        // Given
        final OpenWeatherMapClient client = new OpenWeatherMapClient(
                webClientBuilder, API_KEY, LAT, LON, fixedClockAt(LocalDateTime.of(2026, 8, 16, 7, 0, 0)));
        final OpenWeatherMapClient.ForecastResponse response = new OpenWeatherMapClient.ForecastResponse(List.of(
                entry("2026-08-16 00:00:00", 15.0, 80, 1.0, 800, "clear sky", "01n"),
                entry("2026-08-16 03:00:00", 14.0, 82, 1.2, 800, "clear sky", "01n"),
                entry("2026-08-16 06:00:00", 16.0, 78, 1.5, 800, "clear sky", "01d"),
                entry("2026-08-16 09:00:00", 18.0, 70, 2.0, 800, "clear sky", "01d"),
                entry("2026-08-16 12:00:00", 22.5, 60, 3.5, 500, "light rain", "10d"),
                entry("2026-08-16 15:00:00", 21.0, 65, 3.0, 500, "light rain", "10d"),
                entry("2026-08-16 18:00:00", 19.0, 68, 2.5, 800, "clear sky", "01n"),
                entry("2026-08-16 21:00:00", 17.0, 75, 2.0, 800, "clear sky", "01n")
        ));
        stubWebClientChain(Mono.just(response));

        // When / Then
        StepVerifier.create(client.getHourlyForecast())
                .assertNext(forecast -> {
                    assertEquals(LocalDateTime.of(2026, 8, 16, 9, 0, 0), forecast.dateTime());
                    assertEquals("01d", forecast.iconCode());
                    assertEquals(800, forecast.weatherId());
                    assertEquals("clear sky", forecast.description());
                    assertEquals(18.0, forecast.temperatureC());
                    assertEquals(70.0, forecast.humidityPct());
                    assertEquals(2.0, forecast.windSpeedMs());
                    assertEquals(LAT, forecast.latitude());
                    assertEquals(LON, forecast.longitude());
                })
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 12, 0, 0), forecast.dateTime()))
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 15, 0, 0), forecast.dateTime()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should wrap into the next day when fewer than three entries remain today")
    void getHourlyForecast_ShouldWrapIntoNextDay_WhenFewerThanThreeEntriesRemainToday() {
        // Given
        final OpenWeatherMapClient client = new OpenWeatherMapClient(
                webClientBuilder, API_KEY, LAT, LON, fixedClockAt(LocalDateTime.of(2026, 8, 16, 16, 0, 0)));
        final OpenWeatherMapClient.ForecastResponse response = new OpenWeatherMapClient.ForecastResponse(List.of(
                entry("2026-08-16 18:00:00", 19.0, 68, 2.5, 800, "clear sky", "01n"),
                entry("2026-08-16 21:00:00", 17.0, 75, 2.0, 800, "clear sky", "01n"),
                entry("2026-08-17 00:00:00", 15.0, 80, 1.5, 800, "clear sky", "01n")
        ));
        stubWebClientChain(Mono.just(response));

        // When / Then
        StepVerifier.create(client.getHourlyForecast())
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 18, 0, 0), forecast.dateTime()))
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 21, 0, 0), forecast.dateTime()))
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 17, 0, 0, 0), forecast.dateTime()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should exclude an entry exactly at now and return the following three entries")
    void getHourlyForecast_ShouldExcludeEntryExactlyAtNow() {
        // Given
        final OpenWeatherMapClient client = new OpenWeatherMapClient(
                webClientBuilder, API_KEY, LAT, LON, fixedClockAt(LocalDateTime.of(2026, 8, 16, 12, 0, 0)));
        final OpenWeatherMapClient.ForecastResponse response = new OpenWeatherMapClient.ForecastResponse(List.of(
                entry("2026-08-16 09:00:00", 18.0, 70, 2.0, 800, "clear sky", "01d"),
                entry("2026-08-16 12:00:00", 22.5, 60, 3.5, 500, "light rain", "10d"),
                entry("2026-08-16 15:00:00", 21.0, 65, 3.0, 500, "light rain", "10d"),
                entry("2026-08-16 18:00:00", 19.0, 68, 2.5, 800, "clear sky", "01n"),
                entry("2026-08-16 21:00:00", 17.0, 75, 2.0, 800, "clear sky", "01n")
        ));
        stubWebClientChain(Mono.just(response));

        // When / Then
        StepVerifier.create(client.getHourlyForecast())
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 15, 0, 0), forecast.dateTime()))
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 18, 0, 0), forecast.dateTime()))
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 21, 0, 0), forecast.dateTime()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return fewer than three entries when fewer future entries remain")
    void getHourlyForecast_ShouldReturnFewerThanThree_WhenFewerFutureEntriesRemain() {
        // Given
        final OpenWeatherMapClient client = new OpenWeatherMapClient(
                webClientBuilder, API_KEY, LAT, LON, fixedClockAt(LocalDateTime.of(2026, 8, 16, 20, 0, 0)));
        final OpenWeatherMapClient.ForecastResponse response = new OpenWeatherMapClient.ForecastResponse(List.of(
                entry("2026-08-16 09:00:00", 18.0, 70, 2.0, 800, "clear sky", "01d"),
                entry("2026-08-16 12:00:00", 22.5, 60, 3.5, 500, "light rain", "10d"),
                entry("2026-08-16 15:00:00", 21.0, 65, 3.0, 500, "light rain", "10d"),
                entry("2026-08-16 18:00:00", 19.0, 68, 2.5, 800, "clear sky", "01n"),
                entry("2026-08-16 21:00:00", 17.0, 75, 2.0, 800, "clear sky", "01n")
        ));
        stubWebClientChain(Mono.just(response));

        // When / Then
        StepVerifier.create(client.getHourlyForecast())
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 8, 16, 21, 0, 0), forecast.dateTime()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit an empty Flux when the API returns no hourly forecast entries")
    void getHourlyForecast_ShouldReturnEmpty_WhenListIsEmpty() {
        // Given
        stubWebClientChain(Mono.just(new OpenWeatherMapClient.ForecastResponse(List.of())));

        // When / Then
        StepVerifier.create(openWeatherMapClient.getHourlyForecast())
                .verifyComplete();
    }
}
