package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.WeightEntryDTO;
import com.marvin.nutrition.entity.WeightEntryEntity;
import org.mapstruct.Mapper;

/** MapStruct mapper for converting between {@link WeightEntryEntity} and {@link WeightEntryDTO}. */
@Mapper(componentModel = "spring")
public interface WeightEntryMapper {

    /**
     * Maps a weight entry entity to its DTO representation.
     *
     * @param entity the weight entry entity to map
     * @return the corresponding DTO
     */
    WeightEntryDTO toDTO(WeightEntryEntity entity);
}
