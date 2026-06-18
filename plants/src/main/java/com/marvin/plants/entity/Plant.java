package com.marvin.plants.entity;

import com.marvin.plants.dto.PlantLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "plant", schema = "plants")
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plant_id_gen")
    @SequenceGenerator(name = "plant_id_gen", sequenceName = "plants.plant_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "species", nullable = false)
    private String species;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "care_instructions")
    private String careInstructions;

    @Column(name = "location", nullable = false)
    @Enumerated(EnumType.STRING)
    private PlantLocation location;

    @Column(name = "watering_frequency", nullable = false)
    private Integer wateringFrequency;

    @Column(name = "last_watered_date")
    private LocalDate lastWateredDate;

    @Column(name = "next_watered_date")
    private LocalDate nextWateredDate;

    @Column(name = "fertilizing_frequency")
    private Integer fertilizingFrequency;

    @Column(name = "last_fertilized_date")
    private LocalDate lastFertilizedDate;

    @Column(name = "next_fertilized_date")
    private LocalDate nextFertilizedDate;

    @Column(name = "image")
    private String image;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Plant plant = (Plant) o;
        return Objects.equals(id, plant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
