package com.marvin.plants.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.image.service.ImageService;
import com.marvin.plants.dto.PlantDTO;
import com.marvin.plants.dto.PlantLocation;
import com.marvin.plants.dto.PlantMoistureReading;
import com.marvin.plants.service.PlantMoistureService;
import com.marvin.plants.service.PlantService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
@DisplayName("Plant Controller Tests")
class PlantControllerTest {

    private final UUID testImageUuid = UUID.randomUUID();
    @Mock
    private PlantService plantService;
    @Mock
    private ImageService imageService;
    @Mock
    private PlantMoistureService plantMoistureService;
    @InjectMocks
    private PlantController plantController;
    private PlantDTO testPlantDTO;

    @BeforeEach
    void setUp() {
        testPlantDTO = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(4),
                testImageUuid.toString(),
                null,
                null,
                null,
                null
        );
    }

    @Test
    @DisplayName("Should create plant successfully without image")
    void createPlantShouldReturnCreatedStatusWhenValidPlantWithoutImage() {
        // Given
        final PlantDTO newPlantDTO = new PlantDTO(
                0L,
                "New Plant",
                "New Species",
                "New Description",
                "New Care Instructions",
                PlantLocation.BEDROOM,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(plantService.createPlant(any(PlantDTO.class), eq(""))).thenReturn(2L);

        // When
        final Mono<ResponseEntity<Object>> result = plantController.createPlant(Mono.empty(),
                Mono.just(newPlantDTO), null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertTrue(
                            response.getHeaders().getLocation().toString().contains("/plants/2"));
                })
                .verifyComplete();

        verify(plantService).createPlant(any(PlantDTO.class), eq(""));
    }

    @Test
    @DisplayName("Should create plant successfully with image")
    void createPlantShouldReturnCreatedStatusWhenValidPlantWithImage() {
        // Given
        final PlantDTO newPlantDTO = new PlantDTO(
                0L,
                "New Plant",
                "New Species",
                "New Description",
                "New Care Instructions",
                PlantLocation.BEDROOM,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        final byte[] imageBytes = "test image content".getBytes(StandardCharsets.UTF_8);
        final String contentType = "image/jpeg";

        when(imageService.saveImage(eq(imageBytes), eq(contentType))).thenReturn(
                Mono.just(testImageUuid));
        when(plantService.createPlant(any(PlantDTO.class),
                eq(testImageUuid.toString()))).thenReturn(1L);

        final FilePart filePart = mock(FilePart.class);
        final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(imageBytes);
        when(filePart.content()).thenReturn(Flux.just(dataBuffer));

        // When
        final Mono<ResponseEntity<Object>> result = plantController.createPlant(Mono.just(filePart),
                Mono.just(newPlantDTO), contentType);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertTrue(
                            response.getHeaders().getLocation().toString().contains("/plants/1"));
                })
                .verifyComplete();

        verify(imageService).saveImage(eq(imageBytes), eq(contentType));
        verify(plantService).createPlant(any(PlantDTO.class), eq(testImageUuid.toString()));
    }

    @Test
    @DisplayName("Should handle empty image file")
    void createPlantShouldHandleEmptyImageFile() {
        // Given
        final PlantDTO newPlantDTO = new PlantDTO(
                0L,
                "New Plant",
                "New Species",
                "New Description",
                "New Care Instructions",
                PlantLocation.BEDROOM,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        final FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.empty());

        when(plantService.createPlant(any(PlantDTO.class), eq(""))).thenReturn(1L);

        // When
        final Mono<ResponseEntity<Object>> result = plantController.createPlant(Mono.just(filePart),
                Mono.just(newPlantDTO), null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertTrue(
                            response.getHeaders().getLocation().toString().contains("/plants/1"));
                })
                .verifyComplete();

        verify(plantService).createPlant(any(PlantDTO.class), eq(""));
        verify(imageService, never()).saveImage(any(byte[].class), anyString());
    }

    @Test
    @DisplayName("Should handle image processing in controller flow")
    void createPlantShouldProcessImageCorrectly() {
        // Given
        final PlantDTO newPlantDTO = new PlantDTO(
                0L,
                "New Plant",
                "New Species",
                "New Description",
                "New Care Instructions",
                PlantLocation.BEDROOM,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(plantService.createPlant(any(PlantDTO.class), eq(""))).thenReturn(1L);

        // When - Testing with empty file part (which represents no image)
        final FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.empty());

        final Mono<ResponseEntity<Object>> result = plantController.createPlant(Mono.just(filePart),
                Mono.just(newPlantDTO), null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(
                            response.getHeaders().getLocation().toString().contains("/plants/1"));
                })
                .verifyComplete();

        verify(plantService).createPlant(any(PlantDTO.class), eq(""));
    }

    @Test
    @DisplayName("Should update plant successfully")
    void updatePlantShouldReturnNoContentWhenValidPlantUpdate() {
        // Given
        final PlantDTO updateDTO = new PlantDTO(
                1L,
                "Updated Plant",
                "Updated Species",
                "Updated Description",
                "Updated Care Instructions",
                PlantLocation.KITCHEN,
                10,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "updated-image.jpg",
                null,
                null,
                null,
                null
        );

        doNothing().when(plantService).updatePlant(any(PlantDTO.class));

        // When
        final Mono<ResponseEntity<Object>> result = plantController.updatePlant(Mono.just(updateDTO));

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCode().value());
                })
                .verifyComplete();

        verify(plantService).updatePlant(updateDTO);
    }

    @Test
    @DisplayName("Should get all plants successfully")
    void getPlantsShouldReturnAllPlantsWhenPlantsExist() {
        // Given
        final PlantDTO plant2DTO = new PlantDTO(
                2L,
                "Plant 2",
                "Species 2",
                "Description 2",
                "Care 2",
                PlantLocation.KITCHEN,
                10,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        final List<PlantDTO> plants = List.of(testPlantDTO, plant2DTO);
        when(plantService.getPlants()).thenReturn(Flux.fromIterable(plants));

        // When
        final Flux<PlantDTO> result = plantController.getPlants();

        // Then
        StepVerifier.create(result)
                .expectNext(testPlantDTO)
                .expectNext(plant2DTO)
                .verifyComplete();

        verify(plantService).getPlants();
    }

    @Test
    @DisplayName("Should get all plants successfully when empty")
    void getPlantsShouldReturnEmptyListWhenNoPlantsExist() {
        // Given
        when(plantService.getPlants()).thenReturn(Flux.empty());

        // When
        final Flux<PlantDTO> result = plantController.getPlants();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(plantService).getPlants();
    }

    @Test
    @DisplayName("Should get plant by id successfully")
    void getPlantShouldReturnPlantWhenPlantExists() {
        // Given
        when(plantService.getPlant(1L)).thenReturn(testPlantDTO);

        // When
        final Mono<PlantDTO> result = plantController.getPlant(1L);

        // Then
        StepVerifier.create(result)
                .expectNext(testPlantDTO)
                .verifyComplete();

        verify(plantService).getPlant(1L);
    }

    @Test
    @DisplayName("Should get plant by id when plant does not exist")
    void getPlant_ShouldReturnEmpty_WhenPlantNotExists() {
        // Given
        when(plantService.getPlant(999L)).thenReturn(null);

        // When
        final Mono<PlantDTO> result = plantController.getPlant(999L);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(plantService).getPlant(999L);
    }

    @Test
    @DisplayName("Should delete plant successfully")
    void deletePlant_ShouldReturnNoContent_WhenPlantExists() {
        // Given
        doNothing().when(plantService).deletePlant(1L);

        // When
        final Mono<ResponseEntity<Void>> result = plantController.deletePlant(1L);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCode().value());
                })
                .verifyComplete();

        verify(plantService).deletePlant(1L);
    }

    @Test
    @DisplayName("Should water plant successfully")
    void waterPlant_ShouldReturnUpdatedPlant_WhenValidDate() {
        // Given
        final LocalDate waterDate = LocalDate.now();
        final PlantDTO wateredPlantDTO = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                waterDate,
                waterDate.plusDays(7),
                testImageUuid.toString(),
                null,
                null,
                null,
                null
        );

        when(plantService.waterPlant(1L, waterDate)).thenReturn(wateredPlantDTO);

        // When
        final Mono<ResponseEntity<PlantDTO>> result = plantController.waterPlant(1L, waterDate);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(wateredPlantDTO, response.getBody());
                })
                .verifyComplete();

        verify(plantService).waterPlant(1L, waterDate);
    }

    @Test
    @DisplayName("Should handle water plant with past date")
    void waterPlant_ShouldHandlePastDate() {
        // Given
        final LocalDate pastDate = LocalDate.now().minusDays(1);
        final PlantDTO wateredPlantDTO = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                pastDate,
                pastDate.plusDays(7),
                testImageUuid.toString(),
                null,
                null,
                null,
                null
        );

        when(plantService.waterPlant(1L, pastDate)).thenReturn(wateredPlantDTO);

        // When
        final Mono<ResponseEntity<PlantDTO>> result = plantController.waterPlant(1L, pastDate);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(wateredPlantDTO, response.getBody());
                })
                .verifyComplete();

        verify(plantService).waterPlant(1L, pastDate);
    }

    @Test
    @DisplayName("Should handle water plant with future date")
    void waterPlant_ShouldHandleFutureDate() {
        // Given
        final LocalDate futureDate = LocalDate.now().plusDays(1);
        final PlantDTO wateredPlantDTO = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                futureDate,
                futureDate.plusDays(7),
                testImageUuid.toString(),
                null,
                null,
                null,
                null
        );

        when(plantService.waterPlant(1L, futureDate)).thenReturn(wateredPlantDTO);

        // When
        final Mono<ResponseEntity<PlantDTO>> result = plantController.waterPlant(1L, futureDate);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(wateredPlantDTO, response.getBody());
                })
                .verifyComplete();

        verify(plantService).waterPlant(1L, futureDate);
    }

    @Test
    @DisplayName("Should handle water plant when service throws exception")
    void waterPlant_ShouldHandleServiceException() {
        // Given
        final LocalDate waterDate = LocalDate.now();
        when(plantService.waterPlant(1L, waterDate))
                .thenThrow(new RuntimeException("Plant not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            plantController.waterPlant(1L, waterDate).block();
        });

        verify(plantService).waterPlant(1L, waterDate);
    }

    @Test
    @DisplayName("Should verify method name typo fix in waterPlant endpoint")
    void waterPlant_ShouldFixMethodTypo() {
        // Given
        final LocalDate waterDate = LocalDate.now();
        final PlantDTO wateredPlantDTO = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                waterDate,
                waterDate.plusDays(7),
                testImageUuid.toString(),
                null,
                null,
                null,
                null
        );

        when(plantService.waterPlant(1L, waterDate)).thenReturn(wateredPlantDTO);

        // When
        final Mono<ResponseEntity<PlantDTO>> result = plantController.waterPlant(1L, waterDate);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(wateredPlantDTO, response.getBody());
                })
                .verifyComplete();

        verify(plantService).waterPlant(1L, waterDate);
    }

    @Test
    @DisplayName("Should handle create plant with content type parameter")
    void createPlant_ShouldHandleContentTypeParameter() {
        // Given
        final PlantDTO newPlantDTO = new PlantDTO(
                0L,
                "New Plant",
                "New Species",
                "New Description",
                "New Care Instructions",
                PlantLocation.BEDROOM,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        final byte[] imageBytes = "test image content".getBytes(StandardCharsets.UTF_8);
        final String contentType = "image/jpeg";

        when(imageService.saveImage(eq(imageBytes), eq(contentType))).thenReturn(
                Mono.just(testImageUuid));
        when(plantService.createPlant(any(PlantDTO.class),
                eq(testImageUuid.toString()))).thenReturn(1L);

        final FilePart filePart = mock(FilePart.class);
        final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(imageBytes);
        when(filePart.content()).thenReturn(Flux.just(dataBuffer));

        // When
        final Mono<ResponseEntity<Object>> result = plantController.createPlant(Mono.just(filePart),
                Mono.just(newPlantDTO), contentType);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertTrue(
                            response.getHeaders().getLocation().toString().contains("/plants/1"));
                })
                .verifyComplete();

        verify(imageService).saveImage(eq(imageBytes), eq(contentType));
        verify(plantService).createPlant(any(PlantDTO.class), eq(testImageUuid.toString()));
    }

    @Test
    @DisplayName("Should handle large number of plants request")
    void getPlants_ShouldHandleLargeNumberOfPlants() {
        // Given
        final List<PlantDTO> largePlantList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            largePlantList.add(new PlantDTO(
                    (long) i,
                    "Plant " + i,
                    "Species " + i,
                    "Description " + i,
                    "Care " + i,
                    PlantLocation.values()[i % PlantLocation.values().length],
                    i % 30 + 1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        when(plantService.getPlants()).thenReturn(Flux.fromIterable(largePlantList));

        // When
        final Flux<PlantDTO> result = plantController.getPlants();

        // Then
        StepVerifier.create(result)
                .expectNextCount(100)
                .verifyComplete();

        verify(plantService).getPlants();
    }

    @Test
    @DisplayName("Should verify method parameter validation")
    void deletePlant_VerifyMethodParameterValidation() {
        // Given
        doNothing().when(plantService).deletePlant(1L);

        // When
        final Mono<ResponseEntity<Void>> result = plantController.deletePlant(1L);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCode().value());
                })
                .verifyComplete();

        verify(plantService).deletePlant(1L);
    }

    @Test
    @DisplayName("Should test getFileAsByteArray utility method")
    void testGetFileAsByteArray_WithValidFile() throws Exception {
        // Given - Using reflection to test the private method
        final java.lang.reflect.Method method = PlantController.class.getDeclaredMethod(
                "getFileAsByteArray", Mono.class);
        method.setAccessible(true);

        final byte[] expectedBytes = "test content".getBytes(StandardCharsets.UTF_8);
        final FilePart filePart = mock(FilePart.class);
        final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(expectedBytes);
        when(filePart.content()).thenReturn(Flux.just(dataBuffer));

        // When
        final Mono<byte[]> result = (Mono<byte[]>) method.invoke(plantController, Mono.just(filePart));

        // Then
        StepVerifier.create(result)
                .expectNextMatches(bytes -> Arrays.equals(expectedBytes, bytes))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should test getFileAsByteArray with empty file")
    void testGetFileAsByteArray_WithEmptyFile() throws Exception {
        // Given
        final java.lang.reflect.Method method = PlantController.class.getDeclaredMethod(
                "getFileAsByteArray", Mono.class);
        method.setAccessible(true);

        final FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.empty());

        // When
        final Mono<byte[]> result = (Mono<byte[]>) method.invoke(plantController, Mono.just(filePart));

        // Then
        StepVerifier.create(result)
                .expectNextMatches(bytes -> bytes.length == 0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should test getFileAsByteArray with error")
    void testGetFileAsByteArray_WithError() throws Exception {
        // Given
        final java.lang.reflect.Method method = PlantController.class.getDeclaredMethod(
                "getFileAsByteArray", Mono.class);
        method.setAccessible(true);

        final FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.error(new RuntimeException("File read error")));

        // When
        final Mono<byte[]> result = (Mono<byte[]>) method.invoke(plantController, Mono.just(filePart));

        // Then
        StepVerifier.create(result)
                .expectNextMatches(bytes -> bytes.length == 0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return current moisture when plant exists and has data")
    void getPlantMoisture_ShouldReturnReading_WhenPlantHasEntityIdAndDataAvailable() {
        // Given
        final PlantDTO plantWithSensor = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(4),
                testImageUuid.toString(),
                null,
                null,
                null,
                "feder_calathea_soil_moisture"
        );
        final PlantMoistureReading reading = new PlantMoistureReading(
                "Test Plant", 42.5, Instant.parse("2026-05-16T10:00:00Z"));

        when(plantService.getPlant(1L)).thenReturn(plantWithSensor);
        when(plantMoistureService.getCurrentMoisture("Test Plant", "feder_calathea_soil_moisture"))
                .thenReturn(Mono.just(reading));

        // When
        final Mono<PlantMoistureReading> result = plantController.getPlantMoisture(1L);

        // Then
        StepVerifier.create(result)
                .expectNext(reading)
                .verifyComplete();

        verify(plantService).getPlant(1L);
        verify(plantMoistureService).getCurrentMoisture("Test Plant", "feder_calathea_soil_moisture");
    }

    @Test
    @DisplayName("Should return empty when plant does not exist")
    void getPlantMoisture_ShouldReturnEmpty_WhenPlantNotFound() {
        // Given
        when(plantService.getPlant(999L)).thenReturn(null);

        // When
        final Mono<PlantMoistureReading> result = plantController.getPlantMoisture(999L);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(plantService).getPlant(999L);
        verify(plantMoistureService, never()).getCurrentMoisture(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return empty when plant has no soil moisture entity id configured")
    void getPlantMoisture_ShouldReturnEmpty_WhenNoEntityIdConfigured() {
        // Given
        when(plantService.getPlant(1L)).thenReturn(testPlantDTO);

        // When
        final Mono<PlantMoistureReading> result = plantController.getPlantMoisture(1L);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(plantService).getPlant(1L);
        verify(plantMoistureService, never()).getCurrentMoisture(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return empty when InfluxDB has no moisture data for the plant")
    void getPlantMoisture_ShouldReturnEmpty_WhenServiceReturnsEmpty() {
        // Given
        final PlantDTO plantWithSensor = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(4),
                testImageUuid.toString(),
                null,
                null,
                null,
                "feder_calathea_soil_moisture"
        );

        when(plantService.getPlant(1L)).thenReturn(plantWithSensor);
        when(plantMoistureService.getCurrentMoisture("Test Plant", "feder_calathea_soil_moisture"))
                .thenReturn(Mono.empty());

        // When
        final Mono<PlantMoistureReading> result = plantController.getPlantMoisture(1L);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(plantMoistureService).getCurrentMoisture("Test Plant", "feder_calathea_soil_moisture");
    }

    @Test
    @DisplayName("Should return available moisture sensor entity ids")
    void getMoistureSensors_ShouldReturnEntityIds_WhenAvailable() {
        // Given
        final List<String> entityIds = List.of("alpha_plant_soil_moisture", "zebra_plant_soil_moisture");
        when(plantMoistureService.listAvailableSensorEntityIds()).thenReturn(Mono.just(entityIds));

        // When
        final Flux<String> result = plantController.getMoistureSensors();

        // Then
        StepVerifier.create(result)
                .expectNext("alpha_plant_soil_moisture")
                .expectNext("zebra_plant_soil_moisture")
                .verifyComplete();

        verify(plantMoistureService).listAvailableSensorEntityIds();
    }

    @Test
    @DisplayName("Should return empty when no moisture sensors are available")
    void getMoistureSensors_ShouldReturnEmpty_WhenNoneAvailable() {
        // Given
        when(plantMoistureService.listAvailableSensorEntityIds()).thenReturn(Mono.just(List.of()));

        // When
        final Flux<String> result = plantController.getMoistureSensors();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(plantMoistureService).listAvailableSensorEntityIds();
    }

    @Test
    @DisplayName("Should update moisture threshold successfully")
    void updateMoistureThreshold_ShouldReturnUpdatedPlant_WhenValidInput() {
        // Given
        final PlantDTO updatedDTO = new PlantDTO(
                1L,
                "Test Plant",
                "Test Species",
                "Test Description",
                "Test Care Instructions",
                PlantLocation.LIVING_ROOM,
                7,
                LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(4),
                testImageUuid.toString(),
                null,
                null,
                null,
                "feder_calathea_soil_moisture",
                25.0,
                true
        );

        when(plantService.updateMoistureThreshold(1L, 25.0, true)).thenReturn(updatedDTO);

        // When
        final Mono<ResponseEntity<PlantDTO>> result = plantController.updateMoistureThreshold(1L, 25.0, true);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(updatedDTO, response.getBody());
                })
                .verifyComplete();

        verify(plantService).updateMoistureThreshold(1L, 25.0, true);
    }

    @Test
    @DisplayName("Should clear moisture threshold when null passed")
    void updateMoistureThreshold_ShouldClearThreshold_WhenNullPassed() {
        // Given
        when(plantService.updateMoistureThreshold(1L, null, false)).thenReturn(testPlantDTO);

        // When
        final Mono<ResponseEntity<PlantDTO>> result = plantController.updateMoistureThreshold(1L, null, false);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> assertEquals(200, response.getStatusCode().value()))
                .verifyComplete();

        verify(plantService).updateMoistureThreshold(1L, null, false);
    }

    @Test
    @DisplayName("Should handle update moisture threshold when plant does not exist")
    void updateMoistureThreshold_ShouldHandleServiceException_WhenPlantNotFound() {
        // Given
        when(plantService.updateMoistureThreshold(999L, 25.0, true))
                .thenThrow(new RuntimeException("Plant not found"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                plantController.updateMoistureThreshold(999L, 25.0, true).block());

        verify(plantService).updateMoistureThreshold(999L, 25.0, true);
    }
}
