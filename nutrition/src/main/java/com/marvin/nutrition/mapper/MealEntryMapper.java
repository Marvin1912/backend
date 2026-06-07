package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.entity.MealEntryEntity;
import java.util.List;
import org.mapstruct.Mapper;

/** MapStruct mapper for converting between {@link MealEntryEntity} and {@link MealEntryDTO}. */
@Mapper(componentModel = "spring")
public interface MealEntryMapper {

    /**
     * Maps a meal entry entity to its DTO representation.
     *
     * @param entity the meal entry entity to map
     * @return the corresponding DTO
     */
    MealEntryDTO toDTO(MealEntryEntity entity);

    /**
     * Maps a list of meal entry entities to their DTO representations.
     *
     * @param entities the list of meal entry entities to map
     * @return the list of corresponding DTOs
     */
    List<MealEntryDTO> toDTOList(List<MealEntryEntity> entities);
}
