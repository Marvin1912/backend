package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.ProfileDTO;
import com.marvin.nutrition.entity.ProfileEntity;
import org.mapstruct.Mapper;

/** MapStruct mapper for converting between {@link ProfileEntity} and {@link ProfileDTO}. */
@Mapper(componentModel = "spring")
public interface ProfileMapper {

    /**
     * Maps a profile entity to its DTO representation.
     *
     * @param entity the profile entity to map
     * @return the corresponding DTO
     */
    ProfileDTO toDTO(ProfileEntity entity);

    /**
     * Maps a profile DTO to an entity.
     *
     * @param dto the DTO to map
     * @return the corresponding entity (id will be null for new entities)
     */
    ProfileEntity toEntity(ProfileDTO dto);
}
