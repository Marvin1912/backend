package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.FoodDraftDTO;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Looks up food nutrition data from the OpenFoodFacts API by EAN barcode.
 * Returns a transient {@link FoodDraftDTO} — nothing is persisted.
 * Successful lookups are cached in-memory to avoid redundant HTTP calls.
 */
@Service
public class BarcodeLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(BarcodeLookup.class);
    private static final String USER_AGENT = "backend-nutrition/1.0 (geitnermarvin@googlemail.com)";
    private static final String EAN_PATTERN = "\\d{8,14}";

    /**
     * Maximum number of barcodes retained in the LRU cache.
     * Once this limit is reached the least-recently-accessed entry is evicted,
     * keeping memory consumption bounded regardless of how many distinct barcodes are scanned.
     */
    private static final int MAX_CACHE_ENTRIES = 500;

    private final WebClient webClient;

    /**
     * Bounded LRU cache: access-order {@link LinkedHashMap} capped at {@link #MAX_CACHE_ENTRIES},
     * wrapped in {@link Collections#synchronizedMap} for thread safety.
     */
    private final Map<String, FoodDraftDTO> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, FoodDraftDTO> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            });

    /**
     * Creates a new BarcodeLookup service.
     *
     * @param webClientBuilder the Spring WebClient builder
     * @param baseUrl          the OpenFoodFacts base URL (from {@code nutrition.openfoodfacts.base-url})
     */
    public BarcodeLookup(
            WebClient.Builder webClientBuilder,
            @Value("${nutrition.openfoodfacts.base-url:https://world.openfoodfacts.org}") String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    /**
     * Looks up the food product identified by the given EAN barcode on OpenFoodFacts.
     * The EAN must be 8–14 digits. Results are cached; a cache hit skips the HTTP call.
     *
     * @param ean the EAN-8, EAN-13, or EAN-14 barcode string (digits only)
     * @return a Mono emitting the parsed draft food, or an error if the lookup fails
     */
    public Mono<FoodDraftDTO> lookup(String ean) {
        if (ean == null || ean.isBlank() || !ean.matches(EAN_PATTERN)) {
            return Mono.error(new IllegalArgumentException("Invalid barcode: " + ean));
        }

        return Mono.defer(() -> {
            final FoodDraftDTO cached = cache.get(ean);
            if (cached != null) {
                LOGGER.info("Cache hit for barcode {}", ean);
                return Mono.just(cached);
            }

            LOGGER.info("Looking up barcode {} on OpenFoodFacts", ean);

            return webClient.get()
                    .uri("/api/v2/product/" + ean + ".json?fields=product_name,brands,nutriments,serving_quantity")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorMap(WebClientResponseException.class, e -> {
                        if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                            return new NoSuchElementException("Barcode not found: " + ean);
                        }
                        return new BarcodeLookupException("OpenFoodFacts request failed: " + e.getStatusCode(), e);
                    })
                    .flatMap(response -> parseResponse(response, ean))
                    .doOnSuccess(draft -> {
                        cache.put(ean, draft);
                        LOGGER.info("Barcode lookup succeeded: name={}", draft.name());
                    })
                    .doOnError(e -> LOGGER.error("Barcode lookup failed for EAN {}", ean, e));
        });
    }

    @SuppressWarnings("unchecked")
    private Mono<FoodDraftDTO> parseResponse(Map<?, ?> response, String ean) {
        final Object productObj = response.get("product");
        if (!(productObj instanceof Map)) {
            return Mono.error(new NoSuchElementException("Barcode not found: " + ean));
        }
        final Map<?, ?> product = (Map<?, ?>) productObj;
        final Object nutrimentsObj = product.get("nutriments");
        final Map<?, ?> nutriments = nutrimentsObj instanceof Map ? (Map<?, ?>) nutrimentsObj : Map.of();

        final String name = product.get("product_name") instanceof String s ? s : null;
        final String brandsRaw = product.get("brands") instanceof String s ? s : null;
        final String brand = parseBrand(brandsRaw);

        final BigDecimal kcalPer100 = toBigDecimal(nutriments.get("energy-kcal_100g"));
        final BigDecimal proteinPer100 = toBigDecimal(nutriments.get("proteins_100g"));
        final BigDecimal carbsPer100 = toBigDecimal(nutriments.get("carbohydrates_100g"));
        final BigDecimal fatPer100 = toBigDecimal(nutriments.get("fat_100g"));
        final BigDecimal fiberPer100 = toBigDecimal(nutriments.get("fiber_100g"));
        final BigDecimal servingG = toBigDecimal(product.get("serving_quantity"));

        if (name == null || name.isBlank() || kcalPer100 == null) {
            return Mono.error(new BarcodeLookupException(
                    "OpenFoodFacts returned no usable nutrition data for barcode: " + ean));
        }

        final FoodDraftDTO draft = new FoodDraftDTO(name, brand, kcalPer100, proteinPer100, carbsPer100,
                fatPer100, fiberPer100, servingG);
        return Mono.just(draft);
    }

    private String parseBrand(String brandsRaw) {
        if (brandsRaw == null || brandsRaw.isBlank()) {
            return null;
        }
        final String first = brandsRaw.split(",")[0].trim();
        return first.isBlank() ? null : first;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String s) {
            if (s.isBlank()) {
                return null;
            }
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
