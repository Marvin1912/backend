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
        String soilMoistureEntityId
) {

}
