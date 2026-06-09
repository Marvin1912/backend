package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.entity.MealEntryEntity;
import java.util.List;
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

    /**
     * Maps a list of meal entry entities to their DTO representations with {@code foodName} null.
     * Callers that need resolved food names should use {@link #toDTO(MealEntryEntity, String)} per entry.
     *
     * @param entities the list of meal entry entities to map
     * @return the list of corresponding DTOs with {@code foodName} null for each entry
     */
    @Mapping(target = "foodName", ignore = true)
    List<MealEntryDTO> toDTOList(List<MealEntryEntity> entities);
}
