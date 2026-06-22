package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.entity.SportActivityEntity;
import org.mapstruct.Mapper;

/** MapStruct mapper for converting between {@link SportActivityEntity} and {@link SportActivityDTO}. */
@Mapper(componentModel = "spring")
public interface SportActivityMapper {

    /**
     * Maps a sport activity entity to its DTO representation.
     *
     * @param entity the sport activity entity to map
     * @return the corresponding DTO
     */
    SportActivityDTO toDTO(SportActivityEntity entity);
}
