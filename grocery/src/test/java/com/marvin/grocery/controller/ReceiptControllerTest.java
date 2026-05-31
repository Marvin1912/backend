package com.marvin.grocery.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.ReceiptDTO;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.service.ReceiptService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptController Tests")
class ReceiptControllerTest {

    @Mock
    private ReceiptService receiptService;

    @InjectMocks
    private ReceiptController receiptController;

    private UUID testReceiptId;
    private ReceiptDTO testReceiptDTO;
    private ReceiptItemDTO testItemDTO;

    @BeforeEach
    void setUp() {
        testReceiptId = UUID.randomUUID();
        testItemDTO = new ReceiptItemDTO(1L, "Vollmilch", new BigDecimal("1.09"), 1, new BigDecimal("1.09"));
        testReceiptDTO = new ReceiptDTO(
                testReceiptId,
                LocalDate.of(2024, 3, 15),
                new BigDecimal("1.09"),
                LocalDateTime.now(),
                null
        );
    }

    @Test
    @DisplayName("Should return 201 with Location header after successful upload")
    void uploadReceipt_Success_Returns201WithLocation() {
        final byte[] imageBytes = "fake-image".getBytes(StandardCharsets.UTF_8);
        final FilePart filePart = mock(FilePart.class);
        final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(imageBytes);
        when(filePart.content()).thenReturn(Flux.just(dataBuffer));
        when(receiptService.processAndSave(any(byte[].class))).thenReturn(Mono.just(testReceiptId));

        final Mono<ResponseEntity<Object>> result = receiptController.uploadReceipt(filePart);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(response.getHeaders().getLocation().toString().contains(testReceiptId.toString()));
                })
                .verifyComplete();

        verify(receiptService).processAndSave(any(byte[].class));
    }

    @Test
    @DisplayName("Should return all receipts as Flux")
    void listReceipts_ReturnsFluxOfReceipts() {
        final ReceiptDTO second = new ReceiptDTO(
                UUID.randomUUID(), LocalDate.of(2024, 4, 1), new BigDecimal("5.00"), LocalDateTime.now(), null);
        when(receiptService.findAll()).thenReturn(Flux.just(testReceiptDTO, second));

        final Flux<ReceiptDTO> result = receiptController.listReceipts();

        StepVerifier.create(result)
                .expectNext(testReceiptDTO)
                .expectNext(second)
                .verifyComplete();

        verify(receiptService).findAll();
    }

    @Test
    @DisplayName("Should return 200 with items when receipt exists")
    void getItems_ReceiptExists_Returns200WithItems() {
        when(receiptService.findItems(testReceiptId))
                .thenReturn(Mono.just(Optional.of(List.of(testItemDTO))));

        final Mono<ResponseEntity<List<ReceiptItemDTO>>> result = receiptController.getItems(testReceiptId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(1, response.getBody().size());
                    assertEquals("Vollmilch", response.getBody().get(0).name());
                })
                .verifyComplete();

        verify(receiptService).findItems(testReceiptId);
    }

    @Test
    @DisplayName("Should return 404 when receipt does not exist")
    void getItems_ReceiptNotFound_Returns404() {
        final UUID unknownId = UUID.randomUUID();
        when(receiptService.findItems(unknownId)).thenReturn(Mono.just(Optional.empty()));

        final Mono<ResponseEntity<List<ReceiptItemDTO>>> result = receiptController.getItems(unknownId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(receiptService).findItems(unknownId);
    }

    @Test
    @DisplayName("Should return empty Flux when no receipts exist")
    void listReceipts_NoReceipts_ReturnsEmptyFlux() {
        when(receiptService.findAll()).thenReturn(Flux.empty());

        final Flux<ReceiptDTO> result = receiptController.listReceipts();

        StepVerifier.create(result)
                .verifyComplete();

        verify(receiptService).findAll();
    }

    @Test
    @DisplayName("Should return 204 No Content after successful deletion")
    void deleteReceipt_Exists_Returns204() {
        when(receiptService.deleteReceipt(testReceiptId)).thenReturn(Mono.empty());

        final Mono<ResponseEntity<Void>> result = receiptController.deleteReceipt(testReceiptId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();

        verify(receiptService).deleteReceipt(testReceiptId);
    }

    @Test
    @DisplayName("Should return 404 Not Found when receipt does not exist")
    void deleteReceipt_NotFound_Returns404() {
        final UUID unknownId = UUID.randomUUID();
        when(receiptService.deleteReceipt(unknownId))
                .thenReturn(Mono.error(new NoSuchElementException("Receipt not found: " + unknownId)));

        final Mono<ResponseEntity<Void>> result = receiptController.deleteReceipt(unknownId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(receiptService).deleteReceipt(unknownId);
    }
}
