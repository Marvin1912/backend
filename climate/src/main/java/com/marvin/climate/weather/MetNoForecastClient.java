package com.marvin.climate.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Client for the met.no Locationforecast 2.0 API, providing an hourly forecast. Complies with the met.no terms of service:
 * an identifying {@code User-Agent}, coordinates truncated to four decimals, and caching until {@code Expires} with
 * conditional requests via {@code If-Modified-Since}. All failures yield an empty result so callers can fall back silently.
 */
@Component
public class MetNoForecastClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetNoForecastClient.class);
    private static final String FORECAST_PATH = "/weatherapi/locationforecast/2.0/complete";
    private static final int NEXT_HOURS_COUNT = 4;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final WebClient webClient;
    private final double lat;
    private final double lon;
    private final Clock clock;
    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    /**
     * Creates a new MetNoForecastClient.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param baseUrl          the met.no base URL (from {@code climate.met-no.base-url})
     * @param userAgent        the identifying User-Agent (from {@code climate.met-no.user-agent})
     * @param lat              the forecast location latitude (from {@code climate.openweathermap.lat})
     * @param lon              the forecast location longitude (from {@code climate.openweathermap.lon})
     */
    @Autowired
    public MetNoForecastClient(
            WebClient.Builder webClientBuilder,
            @Value("${climate.met-no.base-url:https://api.met.no}") String baseUrl,
            @Value("${climate.met-no.user-agent:marvin-backend/1.0 github.com/Marvin1912/backend}") String userAgent,
            @Value("${climate.openweathermap.lat:52.5200}") double lat,
            @Value("${climate.openweathermap.lon:13.4050}") double lon
    ) {
        this(webClientBuilder, baseUrl, userAgent, lat, lon, Clock.systemUTC());
    }

    MetNoForecastClient(WebClient.Builder webClientBuilder, String baseUrl, String userAgent, double lat, double lon, Clock clock) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
        this.lat = lat;
        this.lon = lon;
        this.clock = clock;
    }

    /**
     * Returns the next hourly forecast entries strictly after now, sorted ascending. The response is cached until its
     * {@code Expires} time; afterwards a conditional request is sent and a 304 keeps the cached data.
     *
     * @return a Flux emitting up to {@value #NEXT_HOURS_COUNT} entries; empty when met.no is unavailable
     */
    public Flux<HourlyWeatherForecast> getHourlyForecast() {
        return loadForecast()
                .map(this::selectNextHours)
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(e -> {
                    LOGGER.warn("met.no hourly forecast unavailable: {}", e.toString());
                    return Flux.empty();
                });
    }

    private Mono<List<HourlyWeatherForecast>> loadForecast() {
        final CacheEntry cached = cache.get();
        final Instant now = clock.instant();
        if (cached != null && now.isBefore(cached.expires())) {
            return Mono.just(cached.forecasts());
        }
        LOGGER.info("Fetching met.no forecast for lat={}, lon={}", lat, lon);
        return webClient.get()
                .uri(buildUri())
                .headers(headers -> {
                    if (cached != null && cached.lastModified() != null) {
                        headers.set(HttpHeaders.IF_MODIFIED_SINCE, cached.lastModified());
                    }
                })
                .exchangeToMono(response -> handleResponse(response, cached));
    }

    private Mono<List<HourlyWeatherForecast>> handleResponse(final ClientResponse response, final CacheEntry cached) {
        final Instant expires = parseExpires(response.headers().asHttpHeaders().getFirst(HttpHeaders.EXPIRES));
        if (response.statusCode().isSameCodeAs(HttpStatus.NOT_MODIFIED) && cached != null) {
            final CacheEntry refreshed = new CacheEntry(cached.forecasts(), expires, cached.lastModified());
            cache.set(refreshed);
            return response.releaseBody().thenReturn(refreshed.forecasts());
        }
        if (!response.statusCode().is2xxSuccessful()) {
            return response.releaseBody().then(Mono.error(new IllegalStateException("met.no responded with " + response.statusCode())));
        }
        final String lastModified = response.headers().asHttpHeaders().getFirst(HttpHeaders.LAST_MODIFIED);
        return response.bodyToMono(ForecastResponse.class).map(body -> {
            final List<HourlyWeatherForecast> forecasts = toForecasts(body);
            cache.set(new CacheEntry(forecasts, expires, lastModified));
            return forecasts;
        });
    }

    private Instant parseExpires(final String expires) {
        if (expires != null) {
            try {
                return ZonedDateTime.parse(expires, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            } catch (DateTimeParseException e) {
                LOGGER.debug("Unparseable met.no Expires header '{}'", expires);
            }
        }
        return clock.instant().plus(DEFAULT_TTL);
    }

    private String buildUri() {
        return String.format(Locale.ROOT, "%s?lat=%s&lon=%s", FORECAST_PATH, truncate(lat), truncate(lon));
    }

    private static String truncate(final double coordinate) {
        return new BigDecimal(Double.toString(coordinate)).setScale(4, RoundingMode.DOWN).toPlainString();
    }

    private List<HourlyWeatherForecast> selectNextHours(final List<HourlyWeatherForecast> forecasts) {
        final LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        return forecasts.stream()
                .filter(forecast -> forecast.dateTime().isAfter(now))
                .limit(NEXT_HOURS_COUNT)
                .toList();
    }

    private List<HourlyWeatherForecast> toForecasts(final ForecastResponse response) {
        if (response == null || response.properties() == null || response.properties().timeseries() == null) {
            return List.of();
        }
        return response.properties().timeseries().stream()
                .filter(point -> point.time() != null && point.data() != null && point.data().instant() != null
                        && point.data().instant().details() != null && point.data().instant().details().airTemperature() != null)
                .map(this::toForecast)
                .sorted(Comparator.comparing(HourlyWeatherForecast::dateTime))
                .toList();
    }

    private HourlyWeatherForecast toForecast(final TimePoint point) {
        final Details details = point.data().instant().details();
        final String symbol = point.data().next1Hours() == null || point.data().next1Hours().summary() == null
                ? null : point.data().next1Hours().summary().symbolCode();
        final LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.parse(point.time()), ZoneOffset.UTC);
        return new HourlyWeatherForecast(dateTime, symbol, 0, null, details.airTemperature(), details.relativeHumidity(),
                details.windSpeed(), details.airPressureAtSeaLevel(), null, details.dewPointTemperature(), lat, lon);
    }

    private record CacheEntry(List<HourlyWeatherForecast> forecasts, Instant expires, String lastModified) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastResponse(@JsonProperty("properties") Properties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Properties(@JsonProperty("timeseries") List<TimePoint> timeseries) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TimePoint(@JsonProperty("time") String time, @JsonProperty("data") Data data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Data(@JsonProperty("instant") InstantData instant, @JsonProperty("next_1_hours") NextHours next1Hours) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstantData(@JsonProperty("details") Details details) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NextHours(@JsonProperty("summary") Summary summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Summary(@JsonProperty("symbol_code") String symbolCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Details(
            @JsonProperty("air_temperature") Double airTemperature,
            @JsonProperty("air_pressure_at_sea_level") Double airPressureAtSeaLevel,
            @JsonProperty("dew_point_temperature") Double dewPointTemperature,
            @JsonProperty("relative_humidity") Double relativeHumidity,
            @JsonProperty("wind_speed") Double windSpeed
    ) {
    }
}
