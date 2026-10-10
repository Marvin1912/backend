package com.marvin.climate.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link MetNoForecastClient} covering parsing, hour selection, request headers and caching. */
@DisplayName("MetNoForecastClientTest")
class MetNoForecastClientTest {

    private static final double LAT = 52.52;
    private static final double LON = 13.405;
    private static final String USER_AGENT = "test-agent/1.0 example.org";
    private static final Instant NOW = Instant.parse("2026-10-10T12:30:00Z");
    private static final String EXPIRES = "Sat, 10 Oct 2026 13:00:00 GMT";
    private static final String LAST_MODIFIED = "Sat, 10 Oct 2026 12:20:00 GMT";

    /** Clock whose instant can be moved forward during a test. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(final Instant instant) {
            this.instant = instant;
        }

        void advance(final Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static String entry(final String time, final Double temp, final Double pressure, final Double dew, final Double humidity,
            final Double wind) {
        final List<String> details = new ArrayList<>();
        if (temp != null) {
            details.add("\"air_temperature\":" + temp);
        }
        if (pressure != null) {
            details.add("\"air_pressure_at_sea_level\":" + pressure);
        }
        if (dew != null) {
            details.add("\"dew_point_temperature\":" + dew);
        }
        if (humidity != null) {
            details.add("\"relative_humidity\":" + humidity);
        }
        if (wind != null) {
            details.add("\"wind_speed\":" + wind);
        }
        return "{\"time\":\"" + time + "\",\"data\":{\"instant\":{\"details\":{" + String.join(",", details) + "}},"
                + "\"next_1_hours\":{\"summary\":{\"symbol_code\":\"rain\"}}}}";
    }

    private static String hourly(final int fromHourUtc, final int count) {
        final List<String> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int hour = fromHourUtc + i;
            final String time = String.format("2026-10-%02dT%02d:00:00Z", 10 + hour / 24, hour % 24);
            entries.add(entry(time, 10.0 + i, 1010.0 - i, 5.0 + i, 70.0, 3.0));
        }
        return "{\"type\":\"Feature\",\"properties\":{\"timeseries\":[" + String.join(",", entries) + "]}}";
    }

    private static ClientResponse ok(final String json, final String expires) {
        return ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Expires", expires)
                .header("Last-Modified", LAST_MODIFIED)
                .body(json)
                .build();
    }

    private static MetNoForecastClient client(final Function<ClientRequest, Mono<ClientResponse>> handler, final Clock clock) {
        final WebClient.Builder builder = WebClient.builder().exchangeFunction(handler::apply);
        return new MetNoForecastClient(builder, "https://api.met.no", USER_AGENT, LAT, LON, clock);
    }

    private static Clock fixed() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Should select the next four hourly entries strictly after now and map all fields")
    void getHourlyForecast_ShouldSelectNextFourHours_AndMapFields() {
        // 12:00 .. 17:00 UTC, now is 12:30
        final MetNoForecastClient client = client(request -> Mono.just(ok(hourly(12, 6), EXPIRES)), fixed());

        StepVerifier.create(client.getHourlyForecast())
                .assertNext(forecast -> {
                    assertEquals(LocalDateTime.of(2026, 10, 10, 13, 0), forecast.dateTime());
                    assertEquals(11.0, forecast.temperatureC());
                    assertEquals(1009.0, forecast.pressure());
                    assertEquals(6.0, forecast.dewPointC());
                    assertEquals(70.0, forecast.humidityPct());
                    assertEquals(3.0, forecast.windSpeedMs());
                    assertEquals("rain", forecast.iconCode());
                    assertEquals(LAT, forecast.latitude());
                    assertEquals(LON, forecast.longitude());
                })
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 10, 10, 14, 0), forecast.dateTime()))
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 10, 10, 15, 0), forecast.dateTime()))
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 10, 10, 16, 0), forecast.dateTime()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return fewer entries when fewer future entries exist")
    void getHourlyForecast_ShouldReturnFewer_WhenFewFutureEntries() {
        final MetNoForecastClient client = client(request -> Mono.just(ok(hourly(12, 3), EXPIRES)), fixed());

        StepVerifier.create(client.getHourlyForecast().count())
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should leave optional values null and skip entries without a temperature")
    void getHourlyForecast_ShouldHandleMissingValues() {
        final String json = "{\"properties\":{\"timeseries\":["
                + entry("2026-10-10T13:00:00Z", null, 1000.0, null, null, null) + ","
                + entry("2026-10-10T14:00:00Z", 9.0, null, null, null, null) + "]}}";
        final MetNoForecastClient client = client(request -> Mono.just(ok(json, EXPIRES)), fixed());

        StepVerifier.create(client.getHourlyForecast())
                .assertNext(forecast -> {
                    assertEquals(LocalDateTime.of(2026, 10, 10, 14, 0), forecast.dateTime());
                    assertEquals(9.0, forecast.temperatureC());
                    assertNull(forecast.pressure());
                    assertNull(forecast.dewPointC());
                    assertNull(forecast.humidityPct());
                    assertNull(forecast.windSpeedMs());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty for an empty or missing timeseries")
    void getHourlyForecast_ShouldReturnEmpty_WhenNoTimeseries() {
        final MetNoForecastClient client = client(request -> Mono.just(ok("{\"properties\":{}}", EXPIRES)), fixed());

        StepVerifier.create(client.getHourlyForecast()).verifyComplete();
    }

    @Test
    @DisplayName("Should send the identifying User-Agent and coordinates truncated to four decimals")
    void getHourlyForecast_ShouldSendUserAgentAndRoundedCoordinates() {
        final List<ClientRequest> requests = new ArrayList<>();
        final WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            requests.add(request);
            return Mono.just(ok(hourly(12, 6), EXPIRES));
        });
        final MetNoForecastClient client = new MetNoForecastClient(builder, "https://api.met.no", USER_AGENT, 52.5200123, 13.4049876, fixed());

        StepVerifier.create(client.getHourlyForecast()).expectNextCount(4).verifyComplete();

        assertEquals(1, requests.size());
        final ClientRequest request = requests.get(0);
        assertEquals(USER_AGENT, request.headers().getFirst("User-Agent"));
        assertEquals("https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=52.5200&lon=13.4049", request.url().toString());
        assertTrue(request.headers().getFirst("If-Modified-Since") == null);
    }

    @Test
    @DisplayName("Should serve from cache until Expires without a new request")
    void getHourlyForecast_ShouldUseCache_UntilExpires() {
        final AtomicInteger calls = new AtomicInteger();
        final MetNoForecastClient client = client(request -> {
            calls.incrementAndGet();
            return Mono.just(ok(hourly(12, 6), EXPIRES));
        }, fixed());

        StepVerifier.create(client.getHourlyForecast()).expectNextCount(4).verifyComplete();
        StepVerifier.create(client.getHourlyForecast()).expectNextCount(4).verifyComplete();

        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("Should refetch with If-Modified-Since after Expires and reuse the cache on 304")
    void getHourlyForecast_ShouldReuseCache_OnNotModified() {
        final MutableClock clock = new MutableClock(NOW);
        final List<ClientRequest> requests = new ArrayList<>();
        final MetNoForecastClient client = client(request -> {
            requests.add(request);
            if (requests.size() == 1) {
                return Mono.just(ok(hourly(12, 8), EXPIRES));
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_MODIFIED).header("Expires", "Sat, 10 Oct 2026 13:30:00 GMT").build());
        }, clock);

        StepVerifier.create(client.getHourlyForecast()).expectNextCount(4).verifyComplete();

        clock.advance(Duration.ofMinutes(35));
        StepVerifier.create(client.getHourlyForecast())
                .assertNext(forecast -> assertEquals(LocalDateTime.of(2026, 10, 10, 14, 0), forecast.dateTime()))
                .expectNextCount(3)
                .verifyComplete();

        assertEquals(2, requests.size());
        assertEquals(LAST_MODIFIED, requests.get(1).headers().getFirst("If-Modified-Since"));

        // the 304 refreshed Expires to 13:30, so the next call at 13:05 must not hit the network
        StepVerifier.create(client.getHourlyForecast()).expectNextCount(4).verifyComplete();
        assertEquals(2, requests.size());
    }

    @Test
    @DisplayName("Should replace the cache when a refetch returns new data")
    void getHourlyForecast_ShouldReplaceCache_OnNewData() {
        final MutableClock clock = new MutableClock(NOW);
        final AtomicInteger calls = new AtomicInteger();
        final MetNoForecastClient client = client(request -> {
            final int call = calls.incrementAndGet();
            return Mono.just(ok(call == 1 ? hourly(12, 6) : hourly(13, 6), EXPIRES));
        }, clock);

        StepVerifier.create(client.getHourlyForecast()).expectNextCount(4).verifyComplete();
        clock.advance(Duration.ofMinutes(35));
        StepVerifier.create(client.getHourlyForecast().next())
                .assertNext(forecast -> assertEquals(10.0 + 1, forecast.temperatureC()))
                .verifyComplete();
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("Should emit empty instead of an error when the request fails")
    void getHourlyForecast_ShouldReturnEmpty_OnError() {
        final MetNoForecastClient client = client(request -> Mono.error(new IllegalStateException("boom")), fixed());

        StepVerifier.create(client.getHourlyForecast()).verifyComplete();
    }

    @Test
    @DisplayName("Should emit empty instead of an error on a server error status")
    void getHourlyForecast_ShouldReturnEmpty_OnServerError() {
        final MetNoForecastClient client = client(request -> Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build()), fixed());

        StepVerifier.create(client.getHourlyForecast()).verifyComplete();
    }
}
