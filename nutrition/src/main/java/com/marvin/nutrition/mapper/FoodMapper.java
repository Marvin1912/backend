package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.entity.FoodEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper for converting between {@link FoodEntity} and {@link FoodDTO}. */
@Mapper(componentModel = "spring")
public interface FoodMapper {

    /**
     * Maps a food entity to its DTO representation.
     *
     * @param entity the food entity to map
     * @return the corresponding DTO
     */
    FoodDTO toDTO(FoodEntity entity);

    /**
     * Maps a food DTO to an entity. The {@code id} field is always ignored so that
     * the database-generated value is used; a client-supplied id is never honoured.
     *
     * @param dto the DTO to map
     * @return the corresponding entity with a null id (assigned by {@code @GeneratedValue})
     */
    @Mapping(target = "id", ignore = true)
    FoodEntity toEntity(FoodDTO dto);

    /**
     * Maps a list of food entities to their DTO representations.
     *
     * @param entities the list of food entities to map
     * @return the list of corresponding DTOs
     */
    List<FoodDTO> toDTOList(List<FoodEntity> entities);
}
