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
    private final PlantMoistureService plantMoistureService;
    private final Map<Integer, AtomicInteger> wateringStates = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicInteger> fertilizingStates = new ConcurrentHashMap<>();

    public PlantService(
            PlantRepository plantRepository,
            PlantMapper plantMapper,
            MeterRegistry meterRegistry,
            PlantMoistureService plantMoistureService
    ) {
        this.plantRepository = plantRepository;
        this.plantMapper = plantMapper;
        this.meterRegistry = meterRegistry;
        this.plantMoistureService = plantMoistureService;
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

    /**
     * Sets or clears a plant's soil moisture threshold and enables/disables the moisture check.
     *
     * @param id        ID of the plant to update
     * @param threshold soil moisture percentage below which the plant needs water, or {@code null} to clear it
     * @param enabled   whether the moisture check should be active for this plant
     * @return the updated plant data
     */
    @Transactional
    public PlantDTO updateMoistureThreshold(long id, Double threshold, Boolean enabled) {
        final Plant plant = plantRepository.findById(id).orElseThrow();
        plant.setSoilMoistureThreshold(threshold);
        plant.setSoilMoistureCheckEnabled(enabled);

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
                wateringStates.get(plant.getId()).set(needsWater(plant, today) ? 1 : 0)
        );
    }

    private boolean needsWater(Plant plant, LocalDate today) {
        return isScheduleDue(plant, today) || isMoistureBelowThreshold(plant);
    }

    private boolean isScheduleDue(Plant plant, LocalDate today) {
        final LocalDate nextWateredDate = plant.getNextWateredDate();
        return nextWateredDate != null && !nextWateredDate.isAfter(today);
    }

    /**
     * Checks whether a plant's current soil moisture reading is below its configured threshold.
     * Falls back to {@code false} (i.e. relying solely on {@link #isScheduleDue}) whenever the
     * moisture check is disabled, no threshold or sensor is configured, or InfluxDB has no
     * current reading for the sensor — see {@link PlantMoistureService#getCurrentMoisture}.
     *
     * @param plant the plant to check
     * @return {@code true} if the plant's soil moisture is below its threshold
     */
    private boolean isMoistureBelowThreshold(Plant plant) {
        if (Boolean.FALSE.equals(plant.getSoilMoistureCheckEnabled()) || plant.getSoilMoistureThreshold() == null) {
            return false;
        }

        final String entityId = plant.getSoilMoistureEntityId();
        if (entityId == null || entityId.isBlank()) {
            return false;
        }

        return plantMoistureService.getCurrentMoisture(plant.getName(), entityId)
                .map(reading -> reading.moisturePercent() < plant.getSoilMoistureThreshold())
                .blockOptional()
                .orElse(false);
    }

    public void sendFertilizingNotification() {
        final LocalDate today = LocalDate.now();
        plantRepository.findAll().forEach(plant ->
                fertilizingStates.get(plant.getId()).set(plant.getNextFertilizedDate() != null && !plant.getNextFertilizedDate().isAfter(today) ? 1 : 0)
        );
    }
}
