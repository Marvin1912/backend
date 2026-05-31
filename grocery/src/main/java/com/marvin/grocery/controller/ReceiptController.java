package com.marvin.grocery.controller;

import com.marvin.grocery.dto.ReceiptDTO;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for grocery receipt management.
 * Provides endpoints to upload receipt images, list receipts, and retrieve items.
 */
@RestController
@RequestMapping(path = "/receipts")
@Tag(name = "Grocery Receipts", description = "Upload and query grocery receipt data")
public class ReceiptController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptController.class);
    private static final String RECEIPTS_LOCATION_PREFIX = "/receipts/";

    private final ReceiptService receiptService;

    /**
     * Creates a new ReceiptController with the required service.
     *
     * @param receiptService the service handling receipt processing and retrieval
     */
    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /**
     * Uploads a receipt image, runs OCR, parses items, and stores the result.
     *
     * @param filePart the multipart file containing the receipt image
     * @return a Mono with 201 Created and a Location header pointing to the new receipt
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a receipt image",
            description = "Runs OCR on the uploaded image, parses German grocery receipt lines, and persists the result.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Receipt created; Location header contains the URI"),
                @ApiResponse(responseCode = "400", description = "Missing or unreadable file")
            }
    )
    public Mono<ResponseEntity<Object>> uploadReceipt(
            @RequestPart("file")
            @Parameter(description = "Receipt image file (JPEG, PNG, etc.)") FilePart filePart) {
        LOGGER.info("Received receipt upload: filename={}", filePart.filename());
        return extractBytes(filePart)
                .flatMap(receiptService::processAndSave)
                .doOnError(e -> LOGGER.error("Receipt processing failed for file '{}'", filePart.filename(), e))
                .map(uuid -> {
                    final URI location = URI.create(RECEIPTS_LOCATION_PREFIX + uuid);
                    return ResponseEntity.created(location).build();
                });
    }

    /**
     * Lists all stored receipts without item details.
     *
     * @return a Flux emitting all receipt DTOs
     */
    @GetMapping
    @Operation(
            summary = "List all receipts",
            description = "Returns all receipts with id, receiptDate, totalAmount, and creationDate. Items are not included.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Receipt list returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReceiptDTO.class)))
                )
            }
    )
    public Flux<ReceiptDTO> listReceipts() {
        return receiptService.findAll();
    }

    /**
     * Returns the items for a single receipt, or 404 if the receipt does not exist.
     *
     * @param id the UUID of the receipt
     * @return a Mono emitting 200 with the item list, or 404 if not found
     */
    @GetMapping("/{id}/items")
    @Operation(
            summary = "Get items for a receipt",
            description = "Returns all parsed line items for the specified receipt.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Items returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReceiptItemDTO.class)))
                ),
                @ApiResponse(responseCode = "404", description = "Receipt not found")
            }
    )
    public Mono<ResponseEntity<List<ReceiptItemDTO>>> getItems(
            @PathVariable @Parameter(description = "UUID of the receipt") UUID id) {
        return receiptService.findItems(id)
                .map(optItems -> optItems
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    /**
     * Reads all bytes from the given FilePart.
     *
     * @param filePart the file part to read
     * @return a Mono emitting the file bytes
     */
    private Mono<byte[]> extractBytes(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    final byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .doOnError(e -> LOGGER.error("Failed to read uploaded file", e));
    }
}
