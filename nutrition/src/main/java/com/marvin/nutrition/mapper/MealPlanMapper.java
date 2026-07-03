package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.MealPlanChangelogEntryDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.MealPlanStatDTO;
import com.marvin.nutrition.dto.MealPlanTotalsDTO;
import com.marvin.nutrition.entity.MealPlanChangelogEntryEntity;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.entity.MealPlanShoppingCategoryEntity;
import com.marvin.nutrition.entity.MealPlanShoppingItemEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.entity.MealPlanStatEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper converting meal-plan entities into their corresponding DTOs. */
@Mapper(componentModel = "spring")
public interface MealPlanMapper {

    /**
     * Maps a single headline statistic entity to its DTO representation.
     *
     * @param entity the stat entity to map
     * @return the corresponding DTO
     */
    MealPlanStatDTO toStatDTO(MealPlanStatEntity entity);

    /**
     * Maps a list of headline statistic entities to their DTO representations.
     *
     * @param entities the stat entities to map
     * @return the corresponding DTOs
     */
    List<MealPlanStatDTO> toStatDTOs(List<MealPlanStatEntity> entities);

    /**
     * Maps a single changelog entry entity to its DTO representation.
     *
     * @param entity the changelog entry entity to map
     * @return the corresponding DTO
     */
    MealPlanChangelogEntryDTO toChangelogEntryDTO(MealPlanChangelogEntryEntity entity);

    /**
     * Maps a list of changelog entry entities to their DTO representations.
     *
     * @param entities the changelog entry entities to map
     * @return the corresponding DTOs
     */
    List<MealPlanChangelogEntryDTO> toChangelogEntryDTOs(List<MealPlanChangelogEntryEntity> entities);

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
     * Maps a single shopping item entity to its DTO representation.
     *
     * @param entity the shopping item entity to map
     * @return the corresponding DTO
     */
    MealPlanShoppingItemDTO toShoppingItemDTO(MealPlanShoppingItemEntity entity);

    /**
     * Maps a list of shopping item entities to their DTO representations.
     *
     * @param entities the shopping item entities to map
     * @return the corresponding DTOs
     */
    List<MealPlanShoppingItemDTO> toShoppingItemDTOs(List<MealPlanShoppingItemEntity> entities);

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
     * Maps a section entity and its already-mapped rows to a section DTO, folding the entity's
     * nullable totals columns into a nested {@link MealPlanTotalsDTO}.
     *
     * @param entity the section entity to map
     * @param rows   the already-mapped rows belonging to this section
     * @return the corresponding DTO
     */
    @Mapping(target = "rows", source = "rows")
    @Mapping(target = "totals", expression = "java(toTotals(entity))")
    MealPlanSectionDTO toSectionDTO(MealPlanSectionEntity entity, List<MealPlanRowDTO> rows);

    /**
     * Maps a shopping category entity and its already-mapped items to a shopping category DTO.
     *
     * @param entity the shopping category entity to map
     * @param items  the already-mapped items belonging to this category
     * @return the corresponding DTO
     */
    @Mapping(target = "items", source = "items")
    MealPlanShoppingCategoryDTO toShoppingCategoryDTO(MealPlanShoppingCategoryEntity entity, List<MealPlanShoppingItemDTO> items);

    /**
     * Builds the section's nested totals DTO from its scalar totals columns, or {@code null} if the
     * section has no totals row.
     *
     * @param entity the section entity
     * @return the totals DTO, or {@code null} if {@code totalsLabel} is {@code null}
     */
    default MealPlanTotalsDTO toTotals(MealPlanSectionEntity entity) {
        if (entity.getTotalsLabel() == null) {
            return null;
        }
        return new MealPlanTotalsDTO(entity.getTotalsLabel(), entity.getTotalsKcal(), entity.getTotalsProtein());
    }
}
