package com.marvin.plants.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.plants.dto.PlantDTO;
import com.marvin.plants.dto.PlantLocation;
import com.marvin.plants.entity.Plant;
import com.marvin.plants.mapper.PlantMapper;
import com.marvin.plants.repository.PlantRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

    private final String testImageUuid = "test-uuid-123";
    @Mock
    private PlantRepository plantRepository;
    @Mock
    private PlantMapper plantMapper;
    @InjectMocks
    private PlantService plantService;
    private Plant testPlant;
    private PlantDTO testPlantDTO;

    @BeforeEach
    void setUp() {
        testPlant = new Plant();
        testPlant.setId(1);
        testPlant.setName("Test Plant");
        testPlant.setSpecies("Test Species");
        testPlant.setDescription("Test Description");
        testPlant.setCareInstructions("Test Care Instructions");
        testPlant.setLocation(PlantLocation.LIVING_ROOM);
        testPlant.setWateringFrequency(7);
        testPlant.setLastWateredDate(LocalDate.now().minusDays(3));
        testPlant.setNextWateredDate(LocalDate.now().plusDays(4));
        testPlant.setImage("test-image.jpg");

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
                "test-image.jpg",
                null,
                null,
                null
        );
    }

    @Test
    void createPlant_ShouldReturnPlantId_WhenValidInput() {
        // Given
        final PlantDTO plantDto = new PlantDTO(
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
                null
        );

        final Plant newPlant = new Plant();
        newPlant.setId(2);
        newPlant.setName("New Plant");
        newPlant.setSpecies("New Species");
        newPlant.setDescription("New Description");
        newPlant.setCareInstructions("New Care Instructions");
        newPlant.setLocation(PlantLocation.BEDROOM);
        newPlant.setWateringFrequency(5);
        newPlant.setImage(testImageUuid);

        when(plantMapper.toPlant(plantDto, testImageUuid)).thenReturn(newPlant);
        when(plantRepository.save(newPlant)).thenReturn(newPlant);

        // When
        final long result = plantService.createPlant(plantDto, testImageUuid);

        // Then
        assertEquals(2L, result);
        verify(plantMapper).toPlant(plantDto, testImageUuid);
        verify(plantRepository).save(newPlant);
    }

    @Test
    void getPlant_ShouldReturnPlantDTO_WhenPlantExists() {
        // Given
        when(plantRepository.findById(1L)).thenReturn(Optional.of(testPlant));
        when(plantMapper.toPlantDTO(testPlant)).thenReturn(testPlantDTO);

        // When
        final PlantDTO result = plantService.getPlant(1L);

        // Then
        assertNotNull(result);
        assertEquals(testPlantDTO.id(), result.id());
        assertEquals(testPlantDTO.name(), result.name());
        assertEquals(testPlantDTO.species(), result.species());
        verify(plantRepository).findById(1L);
        verify(plantMapper).toPlantDTO(testPlant);
    }

    @Test
    void getPlant_ShouldReturnNull_WhenPlantNotExists() {
        // Given
        when(plantRepository.findById(999L)).thenReturn(Optional.empty());
        when(plantMapper.toPlantDTO(null)).thenReturn(null);

        // When
        final PlantDTO result = plantService.getPlant(999L);

        // Then
        assertNull(result);
        verify(plantRepository).findById(999L);
        verify(plantMapper).toPlantDTO(null);
    }

    @Test
    void getPlants_ShouldReturnAllPlantsAsDTOs() {
        // Given
        final Plant plant2 = new Plant();
        plant2.setId(2);
        plant2.setName("Plant 2");
        plant2.setSpecies("Species 2");
        plant2.setDescription("Description 2");
        plant2.setCareInstructions("Care 2");
        plant2.setLocation(PlantLocation.KITCHEN);
        plant2.setWateringFrequency(10);

        final PlantDTO plantDTO2 = new PlantDTO(
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
                null
        );

        final List<Plant> plants = List.of(testPlant, plant2);
        when(plantRepository.findAll()).thenReturn(plants);
        when(plantMapper.toPlantDTO(testPlant)).thenReturn(testPlantDTO);
        when(plantMapper.toPlantDTO(plant2)).thenReturn(plantDTO2);

        // When
        final var result = plantService.getPlants();

        // Then
        assertNotNull(result);
        final List<PlantDTO> resultList = result.collectList().block();
        assertEquals(2, resultList.size());
        assertEquals(testPlantDTO.name(), resultList.get(0).name());
        assertEquals(plantDTO2.name(), resultList.get(1).name());
        verify(plantRepository).findAll();
        verify(plantMapper, times(2)).toPlantDTO(any(Plant.class));
    }

    @Test
    void deletePlant_ShouldCallRepositoryDeleteById() {
        // When
        plantService.deletePlant(1L);

        // Then
        verify(plantRepository).deleteById(1L);
    }

    @Test
    void updatePlant_ShouldUpdateExistingPlant_WhenPlantExists() {
        // Given
        final LocalDate waterDate = LocalDate.of(2026, 6, 1);
        final LocalDate lastFertilizedDate = LocalDate.of(2026, 6, 1);
        // wateringFrequency=7, fertilizingFrequency=10 => candidate 2026-06-11 => snapped 2026-06-15
        final LocalDate expectedNextFertilizedDate = LocalDate.of(2026, 6, 15);

        final PlantDTO updateDTO = new PlantDTO(
                1L,
                "Updated Plant",
                "Updated Species",
                "Updated Description",
                "Updated Care Instructions",
                PlantLocation.KITCHEN,
                10,
                waterDate,
                waterDate.plusDays(10),
                "updated-image.jpg",
                10,
                lastFertilizedDate,
                null
        );

        testPlant.setWateringFrequency(7);
        testPlant.setFertilizingFrequency(10);
        when(plantRepository.findById(1L)).thenReturn(Optional.of(testPlant));
        doNothing().when(plantMapper).toPlant(testPlant, updateDTO);

        // When
        plantService.updatePlant(updateDTO);

        // Then
        verify(plantRepository).findById(1L);
        verify(plantMapper).toPlant(testPlant, updateDTO);
        assertEquals(waterDate, testPlant.getLastWateredDate());
        // The next watering date should be calculated using the plant's existing watering frequency (7 days)
        assertEquals(waterDate.plusDays(7), testPlant.getNextWateredDate());
        assertEquals(expectedNextFertilizedDate, testPlant.getNextFertilizedDate());
    }

    @Test
    void updatePlant_ShouldThrowException_WhenPlantNotExists() {
        // Given
        final PlantDTO updateDTO = new PlantDTO(
                999L,
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
                null
        );

        when(plantRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> plantService.updatePlant(updateDTO)
        );

        assertEquals("Plant with id 999 not found", exception.getMessage());
        verify(plantRepository).findById(999L);
        verify(plantMapper, never()).toPlant(any(Plant.class), any(PlantDTO.class));
    }

    @Test
    void waterPlant_ShouldUpdateWateringDatesAndReturnDTO_WhenPlantExists() {
        // Given
        final LocalDate waterDate = LocalDate.now();
        final LocalDate expectedNextWaterDate = waterDate.plusDays(testPlant.getWateringFrequency());

        when(plantRepository.findById(1L)).thenReturn(Optional.of(testPlant));
        when(plantMapper.toPlantDTO(testPlant)).thenReturn(testPlantDTO);

        // When
        final PlantDTO result = plantService.waterPlant(1L, waterDate);

        // Then
        assertNotNull(result);
        assertEquals(waterDate, testPlant.getLastWateredDate());
        assertEquals(expectedNextWaterDate, testPlant.getNextWateredDate());
        verify(plantRepository).findById(1L);
        verify(plantMapper).toPlantDTO(testPlant);
    }

    @Test
    void waterPlant_ShouldThrowException_WhenPlantNotExists() {
        // Given
        final LocalDate waterDate = LocalDate.now();
        when(plantRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> plantService.waterPlant(999L, waterDate));
        verify(plantRepository).findById(999L);
        verify(plantMapper, never()).toPlantDTO(any(Plant.class));
    }

    @Test
    void waterPlant_ShouldCalculateNextWateringDateCorrectly() {
        // Given
        final Plant localTestPlant = new Plant();
        localTestPlant.setId(1);
        localTestPlant.setWateringFrequency(5);

        final LocalDate waterDate = LocalDate.of(2023, 6, 15);
        final LocalDate expectedNextWaterDate = LocalDate.of(2023, 6, 20);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(localTestPlant));
        when(plantMapper.toPlantDTO(localTestPlant)).thenReturn(testPlantDTO);

        // When
        plantService.waterPlant(1L, waterDate);

        // Then
        assertEquals(waterDate, localTestPlant.getLastWateredDate());
        assertEquals(expectedNextWaterDate, localTestPlant.getNextWateredDate());
    }

    @Test
    void fertilizePlant_SnapsToNextWateringDate() {
        // Given: lastWatered=2026-06-01, wateringFrequency=7, lastFertilized=2026-06-01, fertilizingFrequency=10
        // candidate = 2026-06-11, which is between watering dates 2026-06-08 and 2026-06-15 => snapped = 2026-06-15
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(7);
        plant.setLastWateredDate(LocalDate.of(2026, 6, 1));
        plant.setFertilizingFrequency(10);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.fertilizePlant(1L, LocalDate.of(2026, 6, 1));

        // Then
        assertEquals(LocalDate.of(2026, 6, 1), plant.getLastFertilizedDate());
        assertEquals(LocalDate.of(2026, 6, 15), plant.getNextFertilizedDate());
    }

    @Test
    void fertilizePlant_AlignsWhenCandidateIsAlreadyOnWateringDate() {
        // Given: lastWatered=2026-06-01, wateringFrequency=5, lastFertilized=2026-06-01, fertilizingFrequency=10
        // candidate = 2026-06-11 = lastWatered + 2*5 => already on schedule => snapped = 2026-06-11
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(5);
        plant.setLastWateredDate(LocalDate.of(2026, 6, 1));
        plant.setFertilizingFrequency(10);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.fertilizePlant(1L, LocalDate.of(2026, 6, 1));

        // Then
        assertEquals(LocalDate.of(2026, 6, 11), plant.getNextFertilizedDate());
    }

    @Test
    void fertilizePlant_SeasonalReset_PushesToAprilFifteenth_WhenSnappedInNovember() {
        // Given: snapped date lands in November => year++ => April 15 of next year
        // lastWatered=2026-10-25, wateringFrequency=7, lastFertilized=2026-10-25, fertilizingFrequency=7
        // candidate=2026-11-01, snapped=2026-11-01 (7 days exactly), month=11 > 10 and >= 10 => year=2027 => 2027-04-15
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(7);
        plant.setLastWateredDate(LocalDate.of(2026, 10, 25));
        plant.setFertilizingFrequency(7);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.fertilizePlant(1L, LocalDate.of(2026, 10, 25));

        // Then
        assertEquals(LocalDate.of(2027, 4, 15), plant.getNextFertilizedDate());
    }

    @Test
    void fertilizePlant_SeasonalReset_PushesToAprilFifteenth_WhenSnappedInFebruary() {
        // Given: snapped date lands in February => month < 4, month < 10 => same year => 2026-04-15
        // lastWatered=2026-01-20, wateringFrequency=7, lastFertilized=2026-01-20, fertilizingFrequency=14
        // candidate=2026-02-03, snapped=2026-02-03 (14 days exactly), month=2 < 4 => 2026-04-15
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(7);
        plant.setLastWateredDate(LocalDate.of(2026, 1, 20));
        plant.setFertilizingFrequency(14);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.fertilizePlant(1L, LocalDate.of(2026, 1, 20));

        // Then
        assertEquals(LocalDate.of(2026, 4, 15), plant.getNextFertilizedDate());
    }

    @Test
    void fertilizePlant_NoOp_WhenLastFertilizedNull() {
        // Given
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(7);
        plant.setLastWateredDate(LocalDate.of(2026, 6, 1));
        plant.setFertilizingFrequency(10);
        plant.setLastFertilizedDate(null);
        plant.setNextFertilizedDate(null);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.fertilizePlant(1L, null);

        // Then
        assertNull(plant.getLastFertilizedDate());
        assertNull(plant.getNextFertilizedDate());
    }

    @Test
    void fertilizePlant_NoOp_WhenFrequencyNull() {
        // Given
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(7);
        plant.setLastWateredDate(LocalDate.of(2026, 6, 1));
        plant.setFertilizingFrequency(null);
        plant.setLastFertilizedDate(null);
        plant.setNextFertilizedDate(null);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.fertilizePlant(1L, LocalDate.of(2026, 6, 1));

        // Then
        assertNull(plant.getLastFertilizedDate());
        assertNull(plant.getNextFertilizedDate());
    }

    @Test
    void waterPlant_RecomputesFertilizingDate_WhenLastFertilizedSet() {
        // Given: plant last watered 2026-06-01, lastFertilized=2026-06-01, fertilizingFrequency=10
        // After waterPlant with newDate=2026-06-08, lastWatered becomes 2026-06-08
        // candidate = 2026-06-01 + 10 = 2026-06-11, snapped against new schedule anchored at 2026-06-08 step 7
        // days=3, n=ceil(3/7)=1, snapped=2026-06-08+7=2026-06-15
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(7);
        plant.setLastWateredDate(LocalDate.of(2026, 6, 1));
        plant.setLastFertilizedDate(LocalDate.of(2026, 6, 1));
        plant.setFertilizingFrequency(10);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.waterPlant(1L, LocalDate.of(2026, 6, 8));

        // Then
        assertEquals(LocalDate.of(2026, 6, 8), plant.getLastWateredDate());
        assertEquals(LocalDate.of(2026, 6, 15), plant.getNextWateredDate());
        assertEquals(LocalDate.of(2026, 6, 15), plant.getNextFertilizedDate());
    }

    @Test
    void waterPlant_LeavesFertilizingUntouched_WhenLastFertilizedNull() {
        // Given
        final Plant plant = new Plant();
        plant.setId(1);
        plant.setWateringFrequency(7);
        plant.setLastWateredDate(LocalDate.of(2026, 6, 1));
        plant.setLastFertilizedDate(null);
        plant.setNextFertilizedDate(null);
        plant.setFertilizingFrequency(10);

        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantMapper.toPlantDTO(plant)).thenReturn(testPlantDTO);

        // When
        plantService.waterPlant(1L, LocalDate.of(2026, 6, 8));

        // Then
        assertNull(plant.getNextFertilizedDate());
    }

    @Test
    void createPlant_ShouldHandleNullImageUuid() {
        // Given
        final PlantDTO plantDto = new PlantDTO(
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
                null
        );

        final Plant newPlant = new Plant();
        newPlant.setId(2);
        newPlant.setName("New Plant");
        newPlant.setSpecies("New Species");
        newPlant.setDescription("New Description");
        newPlant.setCareInstructions("New Care Instructions");
        newPlant.setLocation(PlantLocation.BEDROOM);
        newPlant.setWateringFrequency(5);
        newPlant.setImage(null);

        when(plantMapper.toPlant(plantDto, null)).thenReturn(newPlant);
        when(plantRepository.save(newPlant)).thenReturn(newPlant);

        // When
        final long result = plantService.createPlant(plantDto, null);

        // Then
        assertEquals(2L, result);
        verify(plantMapper).toPlant(plantDto, null);
        verify(plantRepository).save(newPlant);
    }
}
