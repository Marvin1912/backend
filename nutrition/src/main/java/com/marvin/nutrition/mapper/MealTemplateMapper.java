package com.marvin.nutrition.mapper;

import com.marvin.nutrition.dto.MealTemplateItemDTO;
import com.marvin.nutrition.entity.MealTemplateItemEntity;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper for converting {@link MealTemplateItemEntity} into {@link MealTemplateItemDTO}. */
@Mapper(componentModel = "spring")
public interface MealTemplateMapper {

    /**
     * Maps a meal template item entity to its DTO representation, supplying the resolved food name
     * and the live-computed macro values for the item's quantity.
     *
     * @param item     the meal template item entity to map
     * @param foodName the resolved name of the referenced food item
     * @param kcal     the live-computed kilocalories for this item's quantity
     * @param proteinG the live-computed grams of protein for this item's quantity
     * @param carbsG   the live-computed grams of carbohydrates for this item's quantity
     * @param fatG     the live-computed grams of fat for this item's quantity
     * @return the corresponding DTO with resolved food name and macros populated
     */
    @Mapping(target = "foodName", source = "foodName")
    @Mapping(target = "kcal", source = "kcal")
    @Mapping(target = "proteinG", source = "proteinG")
    @Mapping(target = "carbsG", source = "carbsG")
    @Mapping(target = "fatG", source = "fatG")
    MealTemplateItemDTO toItemDTO(
            MealTemplateItemEntity item, String foodName, BigDecimal kcal, BigDecimal proteinG,
            BigDecimal carbsG, BigDecimal fatG);
}
