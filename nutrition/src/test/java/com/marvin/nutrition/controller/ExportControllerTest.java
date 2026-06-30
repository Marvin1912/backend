package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    // -----------------------------------------------------------------------
    // GET /nutrition/export/pdf — happy paths
    // -----------------------------------------------------------------------

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
    @DisplayName("returns 200 for a single-day range when from equals to")
    void exportPdf_FromEqualsTo_Returns200() {
        final LocalDate singleDay = LocalDate.of(2026, 6, 15);
        when(pdfExportService.generatePdf(singleDay, singleDay)).thenReturn(Mono.just(pdfBytes));

        final Mono<ResponseEntity<byte[]>> result = exportController.exportPdf(singleDay, singleDay);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
                    final String contentDisposition =
                            response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                    assertEquals(
                            "attachment; filename=\"nutrition-export-2026-06-15-2026-06-15.pdf\"",
                            contentDisposition);
                })
                .verifyComplete();

        verify(pdfExportService).generatePdf(singleDay, singleDay);
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/export/pdf — error paths
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("throws IllegalArgumentException synchronously when from is after to without calling service")
    void exportPdf_FromAfterTo_ThrowsIllegalArgumentException() {
        final LocalDate laterDate = LocalDate.of(2026, 1, 31);
        final LocalDate earlierDate = LocalDate.of(2026, 1, 1);

        assertThrows(IllegalArgumentException.class,
                () -> exportController.exportPdf(laterDate, earlierDate));

        verify(pdfExportService, never()).generatePdf(laterDate, earlierDate);
    }

    @Test
    @DisplayName("propagates error from service as reactor error signal")
    void exportPdf_ServiceReturnsError_PropagatesError() {
        when(pdfExportService.generatePdf(from, to))
                .thenReturn(Mono.error(new RuntimeException("pdf build failed")));

        StepVerifier.create(exportController.exportPdf(from, to))
                .expectError(RuntimeException.class)
                .verify();
    }
}
