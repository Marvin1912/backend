package com.marvin.grocery.controller;

import com.marvin.grocery.dto.AddReceiptItemRequest;
import com.marvin.grocery.dto.PriceHistoryPointDTO;
import com.marvin.grocery.dto.ProductPriceSummaryDTO;
import com.marvin.grocery.dto.ReceiptDTO;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.dto.UpdateReceiptItemRequest;
import com.marvin.grocery.dto.UpdateSupermarketRequest;
import com.marvin.grocery.service.PriceTrendService;
import com.marvin.grocery.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final PriceTrendService priceTrendService;

    /**
     * Creates a new ReceiptController with the required services.
     *
     * @param receiptService    the service handling receipt processing and retrieval
     * @param priceTrendService the service handling price-history and price-trend aggregation
     */
    public ReceiptController(ReceiptService receiptService, PriceTrendService priceTrendService) {
        this.receiptService = receiptService;
        this.priceTrendService = priceTrendService;
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
     * Manually adds a new item to the given receipt.
     *
     * @param receiptId the UUID of the receipt to add the item to
     * @param request   the new item fields
     * @return a Mono with 201 Created and the new item, or 404 if the receipt is not found
     */
    @PostMapping("/{receiptId}/items")
    @Operation(
            summary = "Add an item to a receipt",
            description = "Manually adds a new line item to the specified receipt.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Item created",
                        content = @Content(schema = @Schema(implementation = ReceiptItemDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Invalid request body (validation failed)"),
                @ApiResponse(responseCode = "404", description = "Receipt not found")
            }
    )
    public Mono<ResponseEntity<ReceiptItemDTO>> addItem(
            @PathVariable @Parameter(description = "UUID of the receipt") UUID receiptId,
            @Valid @RequestBody AddReceiptItemRequest request) {
        return receiptService.addItem(receiptId, request)
                .map(item -> ResponseEntity.status(HttpStatus.CREATED).body(item))
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Updates the editable fields of a single receipt item.
     *
     * @param receiptId the UUID of the parent receipt
     * @param itemId    the id of the item to update
     * @param request   the updated field values
     * @return a Mono with 200 OK and the updated item, or 404 if not found
     */
    @PutMapping("/{receiptId}/items/{itemId}")
    @Operation(
            summary = "Update a receipt item",
            description = "Updates name, quantity, and single price of an item; recalculates the total line price.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Item updated",
                        content = @Content(schema = @Schema(implementation = ReceiptItemDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Invalid request body (validation failed)"),
                @ApiResponse(responseCode = "404", description = "Receipt or item not found")
            }
    )
    public Mono<ResponseEntity<ReceiptItemDTO>> updateItem(
            @PathVariable @Parameter(description = "UUID of the receipt") UUID receiptId,
            @PathVariable @Parameter(description = "Id of the item") Long itemId,
            @Valid @RequestBody UpdateReceiptItemRequest request) {
        return receiptService.updateItem(receiptId, itemId, request)
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Updates the supermarket field of the given receipt.
     *
     * @param id      the UUID of the receipt to update
     * @param request the supermarket selection
     * @return a Mono with 200 OK and the updated receipt DTO, or 404 if not found
     */
    @PatchMapping("/{id}/supermarket")
    @Operation(
            summary = "Set the supermarket for a receipt",
            description = "Updates the supermarket field on an existing receipt.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Supermarket updated",
                        content = @Content(schema = @Schema(implementation = ReceiptDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Receipt not found")
            }
    )
    public Mono<ResponseEntity<ReceiptDTO>> updateSupermarket(
            @PathVariable @Parameter(description = "UUID of the receipt") UUID id,
            @RequestBody UpdateSupermarketRequest request) {
        return receiptService.updateSupermarket(id, request.supermarket())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes the receipt with the given id and all its associated items.
     *
     * @param id the UUID of the receipt to delete
     * @return a Mono with 204 No Content on success, or 404 Not Found if the receipt does not exist
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a receipt",
            description = "Permanently removes the receipt and all its parsed line items.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Receipt deleted"),
                @ApiResponse(responseCode = "404", description = "Receipt not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteReceipt(
            @PathVariable @Parameter(description = "UUID of the receipt to delete") UUID id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        return receiptService.deleteReceipt(id)
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound);
    }

    /**
     * Lists price-trend summaries for every distinct product recorded across all receipts.
     *
     * @return a Flux emitting one price summary per distinct product
     */
    @GetMapping("/products")
    @Operation(
            summary = "List product price trends",
            description = "Returns aggregated first/latest price, percent change, and purchase history for every "
                    + "distinct product, matched by normalized (case-insensitive, trimmed) name.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Product price summaries returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductPriceSummaryDTO.class)))
                )
            }
    )
    public Flux<ProductPriceSummaryDTO> listProductSummaries() {
        return priceTrendService.findAllProductSummaries();
    }

    /**
     * Returns the chronologically ordered price history for a single product.
     *
     * @param name the product name; matched case-insensitively and whitespace-trimmed
     * @return a Mono emitting the ordered list of price-history points, empty if the product was never purchased
     */
    @GetMapping("/products/history")
    @Operation(
            summary = "Get price history for a product",
            description = "Returns the chronologically ordered price history for the given product name, "
                    + "matched by normalized (case-insensitive, trimmed) name.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Price history returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = PriceHistoryPointDTO.class)))
                )
            }
    )
    public Mono<List<PriceHistoryPointDTO>> getProductHistory(
            @RequestParam @Parameter(description = "Product name (case-insensitive, whitespace-trimmed)") String name) {
        return priceTrendService.findHistory(name);
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
