package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.nutrition.dto.FoodDraftDTO;
import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link BarcodeLookup} covering success, caching, and error paths. */
@ExtendWith(MockitoExtension.class)
@DisplayName("BarcodeLookupTest")
class BarcodeLookupTest {

    private static final String VALID_EAN = "3017620422003";

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

    private BarcodeLookup barcodeLookup;

    /** Sets up the WebClient mock chain and creates the service under test. */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.baseUrl(any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(String.class), any(String.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        barcodeLookup = new BarcodeLookup(webClientBuilder, "https://world.openfoodfacts.org", new ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientChain(Mono<?> responseMono) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(String.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseMono).when(responseSpec).bodyToMono(any(Class.class));
    }

    private Map<String, Object> buildOffResponse() {
        final Map<String, Object> nutriments = Map.of(
                "energy-kcal_100g", 539,
                "proteins_100g", 6.3,
                "carbohydrates_100g", 57.5,
                "fat_100g", 30.9,
                "fiber_100g", 0
        );
        final Map<String, Object> product = Map.of(
                "product_name", "Nutella",
                "brands", "Ferrero, Nutella",
                "serving_quantity", 15,
                "nutriments", nutriments
        );
        return Map.of("code", VALID_EAN, "status", 1, "product", product);
    }

    @Test
    @DisplayName("lookup returns parsed FoodDraftDTO with correct per-100g values and first brand")
    void lookup_ValidResponse_ReturnsParsedDTO() {
        stubWebClientChain(Mono.just(buildOffResponse()));

        StepVerifier.create(barcodeLookup.lookup(VALID_EAN))
                .assertNext(dto -> {
                    assertEquals("Nutella", dto.name());
                    assertEquals("Ferrero", dto.brand());
                    assertEquals(0, new BigDecimal("539").compareTo(dto.kcalPer100()));
                    assertEquals(0, new BigDecimal("6.3").compareTo(dto.proteinPer100()));
                    assertEquals(0, new BigDecimal("57.5").compareTo(dto.carbsPer100()));
                    assertEquals(0, new BigDecimal("30.9").compareTo(dto.fatPer100()));
                    assertEquals(0, new BigDecimal("0").compareTo(dto.fiberPer100()));
                    assertEquals(0, new BigDecimal("15").compareTo(dto.servingG()));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("lookup returns FoodDraftDTO with null optional nutriments when fiber and serving absent")
    void lookup_MissingOptionalNutriments_ReturnsNullOptionals() {
        final Map<String, Object> nutriments = Map.of(
                "energy-kcal_100g", 200,
                "proteins_100g", 10.0,
                "carbohydrates_100g", 25.0,
                "fat_100g", 5.0
        );
        final Map<String, Object> product = Map.of(
                "product_name", "Plain Cracker",
                "nutriments", nutriments
        );
        final Map<String, Object> response = Map.of("product", product);
        stubWebClientChain(Mono.just(response));

        StepVerifier.create(barcodeLookup.lookup(VALID_EAN))
                .assertNext(dto -> {
                    assertEquals("Plain Cracker", dto.name());
                    assertNull(dto.brand());
                    assertNull(dto.fiberPer100());
                    assertNull(dto.servingG());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("lookup returns cached result on second call without hitting WebClient again")
    void lookup_CacheHit_DoesNotCallWebClientAgain() {
        stubWebClientChain(Mono.just(buildOffResponse()));

        final Mono<FoodDraftDTO> first = barcodeLookup.lookup(VALID_EAN);
        final Mono<FoodDraftDTO> second = barcodeLookup.lookup(VALID_EAN);

        StepVerifier.create(first).assertNext(dto -> assertEquals("Nutella", dto.name())).verifyComplete();
        StepVerifier.create(second).assertNext(dto -> assertEquals("Nutella", dto.name())).verifyComplete();

        verify(webClient, times(1)).get();
    }

    @Test
    @DisplayName("lookup emits NoSuchElementException when product is absent in response")
    void lookup_ProductAbsent_EmitsNoSuchElementException() {
        final Map<String, Object> response = Map.of("status", 0);
        stubWebClientChain(Mono.just(response));

        StepVerifier.create(barcodeLookup.lookup(VALID_EAN))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("lookup emits NoSuchElementException when API returns HTTP 404")
    void lookup_Http404_EmitsNoSuchElementException() {
        final WebClientResponseException notFound = WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", HttpHeaders.EMPTY, new byte[0], null);
        stubWebClientChain(Mono.error(notFound));

        StepVerifier.create(barcodeLookup.lookup(VALID_EAN))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("lookup emits BarcodeLookupException when API returns HTTP 500")
    void lookup_Http500_EmitsBarcodeLookupException() {
        final WebClientResponseException serverError = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null);
        stubWebClientChain(Mono.error(serverError));

        StepVerifier.create(barcodeLookup.lookup(VALID_EAN))
                .expectError(BarcodeLookupException.class)
                .verify();
    }

    @Test
    @DisplayName("lookup emits BarcodeLookupException when product has no name or kcal")
    void lookup_NoUsableNutritionData_EmitsBarcodeLookupException() {
        final Map<String, Object> product = Map.of("nutriments", Map.of());
        final Map<String, Object> response = Map.of("product", product);
        stubWebClientChain(Mono.just(response));

        StepVerifier.create(barcodeLookup.lookup(VALID_EAN))
                .expectError(BarcodeLookupException.class)
                .verify();
    }

    @Test
    @DisplayName("lookup emits IllegalArgumentException for blank EAN without calling WebClient")
    void lookup_BlankEan_EmitsIllegalArgumentExceptionWithoutWebClientCall() {
        StepVerifier.create(barcodeLookup.lookup(""))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(webClient, times(0)).get();
    }

    @Test
    @DisplayName("lookup emits IllegalArgumentException for non-digit EAN without calling WebClient")
    void lookup_NonDigitEan_EmitsIllegalArgumentExceptionWithoutWebClientCall() {
        StepVerifier.create(barcodeLookup.lookup("abc"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(webClient, times(0)).get();
    }

    @Test
    @DisplayName("lookup emits IllegalArgumentException for too-short EAN without calling WebClient")
    void lookup_TooShortEan_EmitsIllegalArgumentExceptionWithoutWebClientCall() {
        StepVerifier.create(barcodeLookup.lookup("12"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(webClient, times(0)).get();
    }
}
