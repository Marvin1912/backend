package com.marvin.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.marvin.camt.parser.CamtFileParser;
import com.marvin.camt.parser.DocumentUnmarshaller;
import com.marvin.costs.service.CamtImportStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@WebFluxTest(
        controllers = CamtController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@DisplayName("CamtController /camt-entries/import Tests")
class CamtImportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CamtFileParser camtFileParser;

    @MockitoBean
    private DocumentUnmarshaller documentUnmarshaller;

    @MockitoBean
    private CamtImportStorageService camtImportStorageService;

    @Test
    @DisplayName("POST /camt-entries/import with a zip file returns 202 and stores the file")
    void importBookings_ShouldReturn202AndStoreFile_WhenZipUploaded() {
        final MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "fake-zip-content".getBytes())
                .filename("statement.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        webTestClient.post()
                .uri("/camt-entries/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isAccepted();

        verify(camtImportStorageService).store(eq("statement.zip"), any(byte[].class));
    }

    @Test
    @DisplayName("POST /camt-entries/import with a non-zip filename is rejected without storing")
    void importBookings_ShouldReject_WhenFileIsNotZip() {
        final MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "not-a-zip".getBytes())
                .filename("statement.txt")
                .contentType(MediaType.TEXT_PLAIN);

        webTestClient.post()
                .uri("/camt-entries/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().is5xxServerError();

        verifyNoInteractions(camtImportStorageService);
    }
}
