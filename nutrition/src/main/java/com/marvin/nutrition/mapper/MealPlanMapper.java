package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper converting meal-plan entities into their corresponding DTOs. */
@Mapper(componentModel = "spring")
public interface MealPlanMapper {

    /**
     * Maps a single meal row entity to its DTO representation.
     *
     * @param entity the row entity to map
     * @return the corresponding DTO
     */
    MealPlanRowDTO toRowDTO(MealPlanRowEntity entity);

    /**
     * Maps a list of meal row entities to their DTO representations.
     *
     * @param entities the row entities to map
     * @return the corresponding DTOs
     */
    List<MealPlanRowDTO> toRowDTOs(List<MealPlanRowEntity> entities);

    /**
     * Maps a single footer source entity to its DTO representation.
     *
     * @param entity the source entity to map
     * @return the corresponding DTO
     */
    MealPlanSourceDTO toSourceDTO(MealPlanSourceEntity entity);

    /**
     * Maps a list of footer source entities to their DTO representations.
     *
     * @param entities the source entities to map
     * @return the corresponding DTOs
     */
    List<MealPlanSourceDTO> toSourceDTOs(List<MealPlanSourceEntity> entities);

    /**
     * Maps a section entity and its already-mapped rows to a section DTO.
     *
     * @param entity the section entity to map
     * @param rows   the already-mapped rows belonging to this section
     * @return the corresponding DTO
     */
    @Mapping(target = "rows", source = "rows")
    MealPlanSectionDTO toSectionDTO(MealPlanSectionEntity entity, List<MealPlanRowDTO> rows);
}
