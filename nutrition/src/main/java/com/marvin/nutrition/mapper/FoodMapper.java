package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.entity.FoodEntity;
import java.util.List;
import org.mapstruct.Mapper;

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
     * Maps a food DTO to an entity.
     *
     * @param dto the DTO to map
     * @return the corresponding entity (id will be null for new entities)
     */
    FoodEntity toEntity(FoodDTO dto);

    /**
     * Maps a list of food entities to their DTO representations.
     *
     * @param entities the list of food entities to map
     * @return the list of corresponding DTOs
     */
    List<FoodDTO> toDTOList(List<FoodEntity> entities);
}
