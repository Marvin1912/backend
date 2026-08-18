package com.marvin.climate.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Client for the OpenWeatherMap 5-day/3-hour forecast API. Fetches the raw forecast time series
 * server-side (the API key never reaches the frontend) and aggregates it into one representative
 * entry per day - the entry closest to midday - for up to the next three days.
 */
@Component
public class OpenWeatherMapClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenWeatherMapClient.class);
    private static final String FORECAST_PATH = "/data/2.5/forecast";
    private static final DateTimeFormatter DT_TXT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final int MAX_FORECAST_DAYS = 3;

    private final WebClient webClient;
    private final String apiKey;
    private final double lat;
    private final double lon;

    /**
     * Creates a new OpenWeatherMapClient.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param apiKey           the OpenWeatherMap API key (from {@code climate.openweathermap.api-key},
     *                         sourced server-side from the {@code OPENWEATHERMAP_API_KEY} environment variable)
     * @param lat              the forecast location latitude (from {@code climate.openweathermap.lat})
     * @param lon              the forecast location longitude (from {@code climate.openweathermap.lon})
     */
    public OpenWeatherMapClient(
            WebClient.Builder webClientBuilder,
            @Value("${climate.openweathermap.api-key:}") String apiKey,
            @Value("${climate.openweathermap.lat:52.5200}") double lat,
            @Value("${climate.openweathermap.lon:13.4050}") double lon
    ) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.openweathermap.org")
                .build();
        this.apiKey = apiKey;
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Fetches the OpenWeatherMap forecast and aggregates it into one representative entry per day
     * (the entry closest to midday) for up to the next {@value #MAX_FORECAST_DAYS} days, sorted
     * ascending by date.
     *
     * @return a Flux emitting up to {@value #MAX_FORECAST_DAYS} {@link WeatherForecast} entries
     */
    public Flux<WeatherForecast> getForecast() {
        LOGGER.info("Fetching OpenWeatherMap forecast for lat={}, lon={}", lat, lon);

        final String uri = String.format(Locale.ROOT, "%s?lat=%s&lon=%s&appid=%s&units=metric",
                FORECAST_PATH, lat, lon, apiKey);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(ForecastResponse.class)
                .map(this::aggregateByDay)
                .flatMapMany(Flux::fromIterable)
                .doOnError(e -> LOGGER.error("OpenWeatherMap forecast request failed", e));
    }

    private List<WeatherForecast> aggregateByDay(final ForecastResponse response) {
        if (response == null || response.list() == null) {
            return List.of();
        }

        final Map<LocalDate, LocalDateTime> bestTimeByDay = new LinkedHashMap<>();
        final Map<LocalDate, ForecastEntry> bestEntryByDay = new LinkedHashMap<>();
        for (final ForecastEntry entry : response.list()) {
            final LocalDateTime dateTime = LocalDateTime.parse(entry.dtTxt(), DT_TXT_FORMATTER);
            final LocalDate date = dateTime.toLocalDate();
            final LocalDateTime currentBest = bestTimeByDay.get(date);
            if (currentBest == null || isCloserToMidday(dateTime, currentBest)) {
                bestTimeByDay.put(date, dateTime);
                bestEntryByDay.put(date, entry);
            }
        }

        return bestEntryByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(MAX_FORECAST_DAYS)
                .map(dayEntry -> toWeatherForecast(dayEntry.getKey(), dayEntry.getValue()))
                .toList();
    }

    private boolean isCloserToMidday(final LocalDateTime candidate, final LocalDateTime current) {
        final long candidateDiff = Math.abs(Duration.between(candidate.toLocalDate().atTime(LocalTime.NOON), candidate).toMinutes());
        final long currentDiff = Math.abs(Duration.between(current.toLocalDate().atTime(LocalTime.NOON), current).toMinutes());
        return candidateDiff < currentDiff;
    }

    private WeatherForecast toWeatherForecast(final LocalDate date, final ForecastEntry entry) {
        final WeatherInfo weather = entry.weather() == null || entry.weather().isEmpty() ? null : entry.weather().get(0);
        final String iconCode = weather == null ? null : weather.icon();
        final int weatherId = weather == null ? 0 : weather.id();
        final String description = weather == null ? null : weather.description();
        final double temperatureC = entry.main() == null ? 0 : entry.main().temp();
        final Double humidityPct = entry.main() == null ? null : (double) entry.main().humidity();
        final Double windSpeedMs = entry.wind() == null ? null : entry.wind().speed();
        return new WeatherForecast(date, iconCode, weatherId, description, temperatureC, humidityPct, windSpeedMs, lat, lon);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastResponse(@JsonProperty("list") List<ForecastEntry> list) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastEntry(
            @JsonProperty("dt_txt") String dtTxt,
            @JsonProperty("main") MainInfo main,
            @JsonProperty("weather") List<WeatherInfo> weather,
            @JsonProperty("wind") WindInfo wind
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MainInfo(
            @JsonProperty("temp") double temp,
            @JsonProperty("humidity") int humidity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WeatherInfo(
            @JsonProperty("id") int id,
            @JsonProperty("description") String description,
            @JsonProperty("icon") String icon
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WindInfo(@JsonProperty("speed") double speed) {
    }
}
