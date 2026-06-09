package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.entity.MealEntryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper for converting between {@link MealEntryEntity} and {@link MealEntryDTO}. */
@Mapper(componentModel = "spring")
public interface MealEntryMapper {

    /**
     * Maps a meal entry entity to its DTO representation with {@code foodName} set to {@code null}.
     * Intended for ad-hoc entries where no food catalog item is referenced.
     *
     * @param entity the meal entry entity to map
     * @return the corresponding DTO with {@code foodName} null
     */
    @Mapping(target = "foodName", ignore = true)
    MealEntryDTO toDTO(MealEntryEntity entity);

    /**
     * Maps a meal entry entity to its DTO representation, supplying the resolved food name.
     * Intended for food-backed entries where the caller has already resolved the food name.
     *
     * @param entity   the meal entry entity to map
     * @param foodName the resolved name of the referenced food item
     * @return the corresponding DTO with {@code foodName} populated
     */
    @Mapping(target = "foodName", source = "foodName")
    MealEntryDTO toDTO(MealEntryEntity entity, String foodName);
}
