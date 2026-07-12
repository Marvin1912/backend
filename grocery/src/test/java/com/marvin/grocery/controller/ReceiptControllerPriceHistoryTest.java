package com.marvin.grocery.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.common.configuration.jackson.JacksonMapper;
import com.marvin.grocery.dto.ArticleGroupPriceSummaryDTO;
import com.marvin.grocery.dto.PriceHistoryPointDTO;
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
    @DisplayName("GET /receipts/groups returns article group price summaries")
    void listArticleGroupSummaries_ReturnsSummaries() {
        final ArticleGroupPriceSummaryDTO summary = new ArticleGroupPriceSummaryDTO(
                1L,
                "Milch",
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
                .uri("/receipts/groups")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].groupId").isEqualTo(1)
                .jsonPath("$[0].groupName").isEqualTo("Milch")
                .jsonPath("$[0].firstPrice").isEqualTo(1.09)
                .jsonPath("$[0].latestPrice").isEqualTo(1.29)
                .jsonPath("$[0].percentChange").isEqualTo(18.35)
                .jsonPath("$[0].purchaseCount").isEqualTo(2);
    }

    @Test
    @DisplayName("GET /receipts/groups returns an empty array when there are no article groups")
    void listArticleGroupSummaries_NoGroups_ReturnsEmptyArray() {
        when(priceTrendService.findAllProductSummaries()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/receipts/groups")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /receipts/groups/{groupId}/history returns the price history for the given group")
    void getArticleGroupHistory_ReturnsHistory() {
        final UUID receiptId = UUID.randomUUID();
        final PriceHistoryPointDTO point = new PriceHistoryPointDTO(
                LocalDate.of(2026, 1, 1), new BigDecimal("1.09"), 1, Supermarket.LIDL, receiptId);
        when(priceTrendService.findHistory(eq(1L))).thenReturn(Mono.just(List.of(point)));

        webTestClient.get()
                .uri("/receipts/groups/{groupId}/history", 1L)
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
    @DisplayName("GET /receipts/groups/{groupId}/history returns an empty array when there is no history")
    void getArticleGroupHistory_NoHistory_ReturnsEmptyArray() {
        when(priceTrendService.findHistory(eq(2L))).thenReturn(Mono.just(List.of()));

        webTestClient.get()
                .uri("/receipts/groups/{groupId}/history", 2L)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
