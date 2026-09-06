package com.marvin.plants.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object representing a plant and its care requirements")
public record PlantDTO(
        @Schema(description = "Unique identifier of the plant", example = "1")
        long id,
        
        @Schema(description = "Common name of the plant", example = "Monstera Deliciosa")
        String name,
        
        @Schema(description = "Scientific species name", example = "Monstera deliciosa")
        String species,
        
        @Schema(description = "Detailed description of the plant", example = "A large tropical plant with iconic split leaves")
        String description,
        
        @Schema(description = "Specific care instructions for this plant", example = "Keep in bright indirect light and water when top inch of soil is dry")
        String careInstructions,
        
        @Schema(description = "Physical location of the plant in the house")
        PlantLocation location,
        
        @Schema(description = "Watering frequency in days", example = "7")
        Integer wateringFrequency,
        
        @Schema(description = "Date when the plant was last watered")
        LocalDate lastWateredDate,
        
        @Schema(description = "Calculated date for the next watering")
        LocalDate nextWateredDate,
        
        @Schema(description = "UUID of the plant's image", example = "550e8400-e29b-41d4-a716-446655440000")
        String image,
        
        @Schema(description = "Fertilizing frequency in days", example = "30")
        Integer fertilizingFrequency,
        
        @Schema(description = "Date when the plant was last fertilized")
        LocalDate lastFertilizedDate,
        
        @Schema(description = "Calculated date for the next fertilizing")
        LocalDate nextFertilizedDate,

        @Schema(description = "InfluxDB entity_id of the soil moisture sensor for this plant", example = "feder_calathea_soil_moisture")
        String soilMoistureEntityId,

        @Schema(description = "Soil moisture percentage below which the plant is considered to need water", example = "20.0")
        Double soilMoistureThreshold,

        @Schema(description = "Whether the soil moisture check is active for this plant; treated as enabled when null",
                example = "true")
        Boolean soilMoistureCheckEnabled
) {

    /**
     * Preserves the pre-moisture-threshold constructor signature for existing callers;
     * omitting {@code soilMoistureThreshold}/{@code soilMoistureCheckEnabled} falls back to the
     * schedule-only watering logic, consistent with a {@code null} threshold or check-enabled
     * value on the canonical constructor.
     *
     * @param id                   unique identifier of the plant
     * @param name                 common name of the plant
     * @param species              scientific species name
     * @param description          detailed description of the plant
     * @param careInstructions     specific care instructions for this plant
     * @param location             physical location of the plant in the house
     * @param wateringFrequency    watering frequency in days
     * @param lastWateredDate      date when the plant was last watered
     * @param nextWateredDate      calculated date for the next watering
     * @param image                UUID of the plant's image
     * @param fertilizingFrequency fertilizing frequency in days
     * @param lastFertilizedDate   date when the plant was last fertilized
     * @param nextFertilizedDate   calculated date for the next fertilizing
     * @param soilMoistureEntityId InfluxDB entity_id of the soil moisture sensor for this plant
     */
    public PlantDTO(long id, String name, String species, String description, String careInstructions,
            PlantLocation location, Integer wateringFrequency, LocalDate lastWateredDate, LocalDate nextWateredDate,
            String image, Integer fertilizingFrequency, LocalDate lastFertilizedDate, LocalDate nextFertilizedDate,
            String soilMoistureEntityId) {
        this(id, name, species, description, careInstructions, location, wateringFrequency, lastWateredDate,
                nextWateredDate, image, fertilizingFrequency, lastFertilizedDate, nextFertilizedDate,
                soilMoistureEntityId, null, null);
    }
}
