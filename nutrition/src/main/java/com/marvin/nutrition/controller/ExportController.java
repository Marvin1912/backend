package com.marvin.nutrition.controller;

import com.marvin.nutrition.service.PdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for nutrition diary PDF export.
 * Exposes an endpoint that generates and returns a PDF document for a given date range.
 */
@RestController
@Tag(name = "Nutrition", description = "Nutrition profile, weight tracking and target calculation")
public class ExportController {

    private final PdfExportService pdfExportService;

    /**
     * Creates a new ExportController with the required PDF export service.
     *
     * @param pdfExportService the service that generates nutrition diary PDF documents
     */
    public ExportController(PdfExportService pdfExportService) {
        this.pdfExportService = pdfExportService;
    }

    /**
     * Exports the nutrition diary as a downloadable PDF for the given date range.
     * Returns {@code 400 Bad Request} immediately if {@code from} is after {@code to}.
     *
     * @param from the first date to include in the export (ISO-8601, inclusive)
     * @param to   the last date to include in the export (ISO-8601, inclusive)
     * @return a Mono with 200 and the PDF bytes, or 400 if from is after to
     */
    @GetMapping("/nutrition/export/pdf")
    @Operation(
            summary = "Export nutrition diary as PDF",
            description = "Generates a PDF nutrition diary export for the given inclusive date range.",
            responses = {
                @ApiResponse(responseCode = "200", description = "PDF generated and returned as attachment"),
                @ApiResponse(responseCode = "400", description = "from is after to")
            }
    )
    public Mono<ResponseEntity<byte[]>> exportPdf(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date (ISO-8601)", example = "2026-01-01") LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date (ISO-8601)", example = "2026-01-31") LocalDate to) {
        if (from.isAfter(to)) {
            return Mono.just(ResponseEntity.badRequest().<byte[]>build());
        }
        final String filename = "nutrition-export-" + from + "-" + to + ".pdf";
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF);
        responseHeaders.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return pdfExportService.generatePdf(from, to)
                .map(bytes -> ResponseEntity.ok().headers(responseHeaders).body(bytes));
    }
}
