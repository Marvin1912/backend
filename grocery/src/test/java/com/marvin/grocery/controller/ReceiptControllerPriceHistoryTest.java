package com.marvin.grocery.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.common.configuration.jackson.JacksonMapper;
import com.marvin.grocery.dto.PriceHistoryPointDTO;
import com.marvin.grocery.dto.ProductPriceSummaryDTO;
import com.marvin.grocery.entity.Supermarket;
import com.marvin.grocery.service.PriceTrendService;
import com.marvin.grocery.service.ReceiptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptController price-history endpoint Tests")
class ReceiptControllerPriceHistoryTest {

    @Mock
    private ReceiptService receiptService;

    @Mock
    private PriceTrendService priceTrendService;

    @InjectMocks
    private ReceiptController receiptController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new JacksonMapper().objectMapper();
        webTestClient = WebTestClient.bindToController(receiptController)
                .httpMessageCodecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();
    }

    @Test
    @DisplayName("GET /receipts/products returns product price summaries")
    void listProductSummaries_ReturnsSummaries() {
        final ProductPriceSummaryDTO summary = new ProductPriceSummaryDTO(
                "Vollmilch",
                "vollmilch",
                new BigDecimal("1.09"),
                LocalDate.of(2026, 1, 1),
                new BigDecimal("1.29"),
                LocalDate.of(2026, 3, 1),
                new BigDecimal("18.35"),
                2,
                List.of(new BigDecimal("1.09"), new BigDecimal("1.29"))
        );
        when(priceTrendService.findAllProductSummaries()).thenReturn(Flux.just(summary));

        webTestClient.get()
                .uri("/receipts/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Vollmilch")
                .jsonPath("$[0].normalizedName").isEqualTo("vollmilch")
                .jsonPath("$[0].firstPrice").isEqualTo(1.09)
                .jsonPath("$[0].latestPrice").isEqualTo(1.29)
                .jsonPath("$[0].percentChange").isEqualTo(18.35)
                .jsonPath("$[0].purchaseCount").isEqualTo(2);
    }

    @Test
    @DisplayName("GET /receipts/products returns an empty array when there are no products")
    void listProductSummaries_NoProducts_ReturnsEmptyArray() {
        when(priceTrendService.findAllProductSummaries()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/receipts/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /receipts/products/history returns the price history for the given product name")
    void getProductHistory_ReturnsHistory() {
        final UUID receiptId = UUID.randomUUID();
        final PriceHistoryPointDTO point = new PriceHistoryPointDTO(
                LocalDate.of(2026, 1, 1), new BigDecimal("1.09"), 1, Supermarket.LIDL, receiptId);
        when(priceTrendService.findHistory(eq("Vollmilch"))).thenReturn(Mono.just(List.of(point)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/receipts/products/history")
                        .queryParam("name", "Vollmilch")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].date").isEqualTo("2026-01-01")
                .jsonPath("$[0].singlePrice").isEqualTo(1.09)
                .jsonPath("$[0].quantity").isEqualTo(1)
                .jsonPath("$[0].supermarket").isEqualTo("LIDL")
                .jsonPath("$[0].receiptId").isEqualTo(receiptId.toString());
    }

    @Test
    @DisplayName("GET /receipts/products/history handles free-text names with spaces and slashes")
    void getProductHistory_NameWithSpacesAndSlashes_IsPassedThrough() {
        final String freeTextName = "Käse / Gouda 45%";
        when(priceTrendService.findHistory(eq(freeTextName))).thenReturn(Mono.just(List.of()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/receipts/products/history")
                        .queryParam("name", freeTextName)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
