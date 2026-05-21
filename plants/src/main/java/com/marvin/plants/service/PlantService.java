package com.marvin.plants.service;

import com.marvin.plants.dto.PlantDTO;
import com.marvin.plants.entity.Plant;
import com.marvin.plants.mapper.PlantMapper;
import com.marvin.plants.repository.PlantRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final PlantMapper plantMapper;
    private final MeterRegistry meterRegistry;
    private final Map<Integer, AtomicInteger> wateringStates = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicInteger> fertilizingStates = new ConcurrentHashMap<>();

    public PlantService(
            PlantRepository plantRepository,
            PlantMapper plantMapper,
            MeterRegistry meterRegistry
    ) {
        this.plantRepository = plantRepository;
        this.plantMapper = plantMapper;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initGauges() {
        plantRepository.findAll().forEach(plant -> {
            final AtomicInteger waterState = wateringStates.computeIfAbsent(plant.getId(), id -> new AtomicInteger(0));
            Gauge.builder("water_plant", waterState, AtomicInteger::get)
                    .tag("plant", plant.getName())
                    .register(meterRegistry);

            final AtomicInteger fertilizeState = fertilizingStates.computeIfAbsent(plant.getId(), id -> new AtomicInteger(0));
            Gauge.builder("fertilize_plant", fertilizeState, AtomicInteger::get)
                    .tag("plant", plant.getName())
                    .register(meterRegistry);
        });
    }

    @Transactional
    public long createPlant(PlantDTO plantDto, String imageUuid) {
        return plantRepository.save(plantMapper.toPlant(plantDto, imageUuid)).getId();
    }

    public PlantDTO getPlant(long id) {
        final Plant plant = plantRepository.findById(id).orElse(null);
        return plantMapper.toPlantDTO(plant);
    }

    public Flux<PlantDTO> getPlants() {
        return Flux.fromIterable(plantRepository.findAll()).map(plantMapper::toPlantDTO);
    }

    public void deletePlant(long id) {
        plantRepository.deleteById(id);
    }

    @Transactional
    public void updatePlant(PlantDTO dto) {
        plantRepository.findById(dto.id()).ifPresentOrElse(
                plant -> {
                    plantMapper.toPlant(plant, dto);
                    waterPlant(plant, dto.lastWateredDate());
                    fertilizePlant(plant, dto.lastFertilizedDate());
                },
                () -> {
                    throw new IllegalArgumentException(
                            "Plant with id %s not found".formatted(dto.id()));
                }
        );
    }

    @Transactional
    public PlantDTO waterPlant(long id, LocalDate lastWatered) {

        final Plant plant = plantRepository.findById(id).orElseThrow();
        waterPlant(plant, lastWatered);

        return plantMapper.toPlantDTO(plant);
    }

    @Transactional
    public PlantDTO fertilizePlant(long id, LocalDate lastFertilized) {

        final Plant plant = plantRepository.findById(id).orElseThrow();
        fertilizePlant(plant, lastFertilized);

        return plantMapper.toPlantDTO(plant);
    }

    private void waterPlant(Plant plant, LocalDate lastWatered) {
        plant.setLastWateredDate(lastWatered);
        plant.setNextWateredDate(lastWatered.plusDays(plant.getWateringFrequency()));
        if (plant.getLastFertilizedDate() != null && plant.getFertilizingFrequency() != null) {
            fertilizePlant(plant, plant.getLastFertilizedDate());
        }
    }

    private void fertilizePlant(Plant plant, LocalDate lastFertilized) {
        if (lastFertilized != null && plant.getFertilizingFrequency() != null) {
            plant.setLastFertilizedDate(lastFertilized);
            final LocalDate candidate = lastFertilized.plusDays(plant.getFertilizingFrequency());
            LocalDate snapped = snapToWateringDate(candidate, plant.getLastWateredDate(), plant.getWateringFrequency());

            // Fertilizing period is April (month 4) to October (month 10)
            // If next fertilizing date is outside this range, set to April 15th
            final int month = snapped.getMonthValue();
            if (month < 4 || month > 10) {
                int year = snapped.getYear();
                // If current month is October, November, or December, increment year
                if (month >= 10) {
                    year++;
                }
                snapped = LocalDate.of(year, 4, 15);
            }

            plant.setNextFertilizedDate(snapped);
        }
    }

    private LocalDate snapToWateringDate(LocalDate candidate, LocalDate lastWatered, int wateringFrequency) {
        if (!candidate.isAfter(lastWatered)) {
            return lastWatered.plusDays(wateringFrequency);
        }
        final long days = ChronoUnit.DAYS.between(lastWatered, candidate);
        final long n = (days + wateringFrequency - 1) / wateringFrequency;
        return lastWatered.plusDays(n * wateringFrequency);
    }

    public void sendWateringNotification() {
        final LocalDate today = LocalDate.now();
        plantRepository.findAll().forEach(plant ->
                wateringStates.get(plant.getId()).set(!plant.getNextWateredDate().isAfter(today) ? 1 : 0)
        );
    }

    public void sendFertilizingNotification() {
        final LocalDate today = LocalDate.now();
        plantRepository.findAll().forEach(plant ->
                fertilizingStates.get(plant.getId()).set(plant.getNextFertilizedDate() != null && !plant.getNextFertilizedDate().isAfter(today) ? 1 : 0)
        );
    }
}
