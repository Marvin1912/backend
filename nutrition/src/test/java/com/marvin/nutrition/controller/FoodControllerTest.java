package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.dto.FoodDraftDTO;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.service.BarcodeLookup;
import com.marvin.nutrition.service.BarcodeLookupException;
import com.marvin.nutrition.service.FoodService;
import com.marvin.nutrition.service.LabelReadException;
import com.marvin.nutrition.service.LabelReader;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link FoodController} covering all CRUD, search and scan-label endpoints. */
@ExtendWith(MockitoExtension.class)
@DisplayName("FoodController Tests")
class FoodControllerTest {

    @Mock
    private FoodService foodService;

    @Mock
    private LabelReader labelReader;

    @Mock
    private BarcodeLookup barcodeLookup;

    @Mock
    private FilePart filePart;

    @InjectMocks
    private FoodController foodController;

    private UUID testId;
    private FoodDTO testFoodDTO;

    /** Sets up test fixtures shared across all test methods. */
    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testFoodDTO = new FoodDTO(
                testId,
                "Chicken Breast",
                "Brand A",
                new BigDecimal("165.00"),
                new BigDecimal("31.00"),
                new BigDecimal("0.00"),
                new BigDecimal("3.60"),
                null,
                new BigDecimal("100.00"),
                FoodSource.MANUAL
        );
    }

    @Test
    @DisplayName("Should return all foods as Flux when no query parameter is given")
    void listFoods_NoQuery_ReturnsAllFoods() {
        final FoodDTO second = new FoodDTO(
                UUID.randomUUID(), "Oats", null,
                new BigDecimal("389.00"), new BigDecimal("17.00"),
                new BigDecimal("66.00"), new BigDecimal("7.00"),
                new BigDecimal("10.00"), new BigDecimal("40.00"),
                FoodSource.MANUAL
        );
        when(foodService.findAll(null, 0, 50)).thenReturn(Flux.just(testFoodDTO, second));

        final Flux<FoodDTO> result = foodController.listFoods(null, 0, 50);

        StepVerifier.create(result)
                .expectNext(testFoodDTO)
                .expectNext(second)
                .verifyComplete();

        verify(foodService).findAll(null, 0, 50);
    }

    @Test
    @DisplayName("Should return matching foods as Flux when query parameter is provided")
    void listFoods_WithQuery_ReturnsMatchingFoods() {
        when(foodService.findAll("chicken", 0, 50)).thenReturn(Flux.just(testFoodDTO));

        final Flux<FoodDTO> result = foodController.listFoods("chicken", 0, 50);

        StepVerifier.create(result)
                .expectNext(testFoodDTO)
                .verifyComplete();

        verify(foodService).findAll("chicken", 0, 50);
    }

    @Test
    @DisplayName("Should return empty Flux when no foods match the query")
    void listFoods_NoMatch_ReturnsEmptyFlux() {
        when(foodService.findAll("xyz", 0, 50)).thenReturn(Flux.empty());

        final Flux<FoodDTO> result = foodController.listFoods("xyz", 0, 50);

        StepVerifier.create(result)
                .verifyComplete();

        verify(foodService).findAll("xyz", 0, 50);
    }

    @Test
    @DisplayName("Should return 200 with food DTO when food exists")
    void getFoodById_Found_Returns200WithDTO() {
        when(foodService.findById(testId)).thenReturn(Mono.just(testFoodDTO));

        final Mono<ResponseEntity<FoodDTO>> result = foodController.getFoodById(testId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Chicken Breast", response.getBody().name());
                })
                .verifyComplete();

        verify(foodService).findById(testId);
    }

    @Test
    @DisplayName("Should return 404 when food is not found by id")
    void getFoodById_NotFound_Returns404() {
        final UUID unknownId = UUID.randomUUID();
        when(foodService.findById(unknownId))
                .thenReturn(Mono.error(new NoSuchElementException("Food not found: " + unknownId)));

        final Mono<ResponseEntity<FoodDTO>> result = foodController.getFoodById(unknownId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(foodService).findById(unknownId);
    }

    @Test
    @DisplayName("Should return 201 Created with Location header after creating a food")
    void createFood_Valid_Returns201WithLocation() {
        when(foodService.create(any(FoodDTO.class))).thenReturn(Mono.just(testFoodDTO));

        final Mono<ResponseEntity<FoodDTO>> result = foodController.createFood(testFoodDTO);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(response.getHeaders().getLocation().toString()
                            .contains(testId.toString()));
                })
                .verifyComplete();

        verify(foodService).create(any(FoodDTO.class));
    }

    @Test
    @DisplayName("Should return 200 with updated DTO after updating a food")
    void updateFood_Found_Returns200WithUpdatedDTO() {
        final FoodDTO updated = new FoodDTO(
                testId, "Chicken Breast Grilled", "Brand A",
                new BigDecimal("165.00"), new BigDecimal("31.00"),
                new BigDecimal("0.00"), new BigDecimal("3.60"),
                null, new BigDecimal("150.00"), FoodSource.MANUAL
        );
        when(foodService.update(eq(testId), any(FoodDTO.class))).thenReturn(Mono.just(updated));

        final Mono<ResponseEntity<FoodDTO>> result = foodController.updateFood(testId, updated);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Chicken Breast Grilled", response.getBody().name());
                })
                .verifyComplete();

        verify(foodService).update(eq(testId), any(FoodDTO.class));
    }

    @Test
    @DisplayName("Should return 404 when food to update does not exist")
    void updateFood_NotFound_Returns404() {
        final UUID unknownId = UUID.randomUUID();
        when(foodService.update(eq(unknownId), any(FoodDTO.class)))
                .thenReturn(Mono.error(new NoSuchElementException("Food not found: " + unknownId)));

        final Mono<ResponseEntity<FoodDTO>> result = foodController.updateFood(unknownId, testFoodDTO);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(foodService).update(eq(unknownId), any(FoodDTO.class));
    }

    @Test
    @DisplayName("Should return 204 No Content after successfully deleting a food")
    void deleteFood_Exists_Returns204() {
        when(foodService.delete(testId)).thenReturn(Mono.empty());

        final Mono<ResponseEntity<Void>> result = foodController.deleteFood(testId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();

        verify(foodService).delete(testId);
    }

    @Test
    @DisplayName("Should return 404 when food to delete does not exist")
    void deleteFood_NotFound_Returns404() {
        final UUID unknownId = UUID.randomUUID();
        when(foodService.delete(unknownId))
                .thenReturn(Mono.error(new NoSuchElementException("Food not found: " + unknownId)));

        final Mono<ResponseEntity<Void>> result = foodController.deleteFood(unknownId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(foodService).delete(unknownId);
    }

    @Test
    @DisplayName("scanLabel returns 200 with draft DTO when label reader succeeds")
    void scanLabel_Success_Returns200WithDraft() {
        final FoodDraftDTO draft = new FoodDraftDTO(
                "Müsli", "Kellogg's",
                new BigDecimal("370.0"), new BigDecimal("8.5"),
                new BigDecimal("67.0"), new BigDecimal("6.0"),
                new BigDecimal("5.5"), new BigDecimal("45.0")
        );

        final byte[] imageBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(imageBytes);

        when(filePart.content()).thenReturn(Flux.just(dataBuffer));
        when(labelReader.readLabel(any(byte[].class))).thenReturn(Mono.just(draft));

        final Mono<ResponseEntity<FoodDraftDTO>> result = foodController.scanLabel(filePart);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Müsli", response.getBody().name());
                    assertEquals("Kellogg's", response.getBody().brand());
                    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
                })
                .verifyComplete();

        verify(labelReader).readLabel(any(byte[].class));
    }

    @Test
    @DisplayName("scanLabel propagates LabelReadException when label reader fails")
    void scanLabel_LabelReadException_PropagatesError() {
        final byte[] imageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8};
        final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(imageBytes);

        when(filePart.content()).thenReturn(Flux.just(dataBuffer));
        when(labelReader.readLabel(any(byte[].class)))
                .thenReturn(Mono.error(new LabelReadException("Claude returned non-JSON response")));

        final Mono<ResponseEntity<FoodDraftDTO>> result = foodController.scanLabel(filePart);

        StepVerifier.create(result)
                .expectError(LabelReadException.class)
                .verify();
    }

    @Test
    @DisplayName("lookupBarcode returns 200 with draft DTO when barcode lookup succeeds")
    void lookupBarcode_Success_Returns200WithDraft() {
        final FoodDraftDTO draft = new FoodDraftDTO(
                "Nutella", "Ferrero",
                new BigDecimal("539"), new BigDecimal("6.3"),
                new BigDecimal("57.5"), new BigDecimal("30.9"),
                new BigDecimal("0"), new BigDecimal("15")
        );
        when(barcodeLookup.lookup("3017620422003")).thenReturn(Mono.just(draft));

        final Mono<ResponseEntity<FoodDraftDTO>> result = foodController.lookupBarcode("3017620422003");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Nutella", response.getBody().name());
                    assertEquals("Ferrero", response.getBody().brand());
                    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
                })
                .verifyComplete();

        verify(barcodeLookup).lookup("3017620422003");
    }

    @Test
    @DisplayName("lookupBarcode propagates NoSuchElementException when barcode is not found")
    void lookupBarcode_NotFound_PropagatesNoSuchElementException() {
        when(barcodeLookup.lookup("9999999999999"))
                .thenReturn(Mono.error(new NoSuchElementException("Barcode not found: 9999999999999")));

        final Mono<ResponseEntity<FoodDraftDTO>> result = foodController.lookupBarcode("9999999999999");

        StepVerifier.create(result)
                .expectError(NoSuchElementException.class)
                .verify();

        verify(barcodeLookup).lookup("9999999999999");
    }

    @Test
    @DisplayName("lookupBarcode propagates BarcodeLookupException when no usable nutrition data")
    void lookupBarcode_NoUsableData_PropagatesBarcodeLookupException() {
        when(barcodeLookup.lookup("3017620422003"))
                .thenReturn(Mono.error(new BarcodeLookupException("OpenFoodFacts returned no usable nutrition data")));

        final Mono<ResponseEntity<FoodDraftDTO>> result = foodController.lookupBarcode("3017620422003");

        StepVerifier.create(result)
                .expectError(BarcodeLookupException.class)
                .verify();

        verify(barcodeLookup).lookup("3017620422003");
    }
}
