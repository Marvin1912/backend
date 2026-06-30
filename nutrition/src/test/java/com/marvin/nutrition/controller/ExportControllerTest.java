package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.service.PdfExportService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link ExportController} covering the PDF export endpoint. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExportController Tests")
class ExportControllerTest {

    @Mock
    private PdfExportService pdfExportService;

    @InjectMocks
    private ExportController exportController;

    private LocalDate from;
    private LocalDate to;
    private byte[] pdfBytes;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        from = LocalDate.of(2026, 1, 1);
        to = LocalDate.of(2026, 1, 31);
        pdfBytes = new byte[]{1, 2, 3, 4, 5};
    }

    @Test
    @DisplayName("returns 200 with PDF content type and attachment header when service returns bytes")
    void exportPdf_ServiceReturnsBytes_Returns200WithPdfContentTypeAndAttachmentHeader() {
        when(pdfExportService.generatePdf(from, to)).thenReturn(Mono.just(pdfBytes));

        final Mono<ResponseEntity<byte[]>> result = exportController.exportPdf(from, to);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
                    final String contentDisposition =
                            response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                    assertEquals(
                            "attachment; filename=\"nutrition-export-2026-01-01-2026-01-31.pdf\"",
                            contentDisposition);
                    assertArrayEquals(pdfBytes, response.getBody());
                })
                .verifyComplete();

        verify(pdfExportService).generatePdf(from, to);
    }

    @Test
    @DisplayName("returns 400 Bad Request when from is after to without calling service")
    void exportPdf_FromAfterTo_Returns400WithoutCallingService() {
        final LocalDate laterDate = LocalDate.of(2026, 1, 31);
        final LocalDate earlierDate = LocalDate.of(2026, 1, 1);

        final Mono<ResponseEntity<byte[]>> result = exportController.exportPdf(laterDate, earlierDate);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(400, response.getStatusCode().value()))
                .verifyComplete();

        verify(pdfExportService, never()).generatePdf(any(LocalDate.class), any(LocalDate.class));
    }
}
