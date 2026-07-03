package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.entity.MealPlanShoppingCategoryEntity;
import com.marvin.nutrition.entity.MealPlanShoppingItemEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanShoppingCategoryRepository;
import com.marvin.nutrition.repository.MealPlanShoppingItemRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Assembles the meal plan's ordered list of shopping categories together with each category's ordered items. */
@Component
public class MealPlanShoppingListAssembler {

    private final MealPlanShoppingCategoryRepository mealPlanShoppingCategoryRepository;
    private final MealPlanShoppingItemRepository mealPlanShoppingItemRepository;
    private final MealPlanMapper mealPlanMapper;

    /**
     * Creates a new MealPlanShoppingListAssembler.
     *
     * @param mealPlanShoppingCategoryRepository JPA repository for shopping categories
     * @param mealPlanShoppingItemRepository     JPA repository for shopping items
     * @param mealPlanMapper                     mapper for converting entities into DTOs
     */
    public MealPlanShoppingListAssembler(
            MealPlanShoppingCategoryRepository mealPlanShoppingCategoryRepository,
            MealPlanShoppingItemRepository mealPlanShoppingItemRepository,
            MealPlanMapper mealPlanMapper) {
        this.mealPlanShoppingCategoryRepository = mealPlanShoppingCategoryRepository;
        this.mealPlanShoppingItemRepository = mealPlanShoppingItemRepository;
        this.mealPlanMapper = mealPlanMapper;
    }

    /**
     * Loads and assembles all shopping categories of the given meal plan, each with its ordered items.
     *
     * @param mealPlanId the id of the meal plan
     * @return the ordered list of shopping category DTOs
     */
    public List<MealPlanShoppingCategoryDTO> assemble(Long mealPlanId) {
        final List<MealPlanShoppingCategoryEntity> categories =
                mealPlanShoppingCategoryRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlanId);
        return categories.stream()
                .map(this::toShoppingCategoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Loads a category's items and maps the category together with them to a DTO.
     *
     * @param category the shopping category entity
     * @return the assembled shopping category DTO
     */
    private MealPlanShoppingCategoryDTO toShoppingCategoryDTO(MealPlanShoppingCategoryEntity category) {
        final List<MealPlanShoppingItemEntity> itemEntities =
                mealPlanShoppingItemRepository.findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(category.getId());
        final List<MealPlanShoppingItemDTO> items = mealPlanMapper.toShoppingItemDTOs(itemEntities);
        return mealPlanMapper.toShoppingCategoryDTO(category, items);
    }
}
