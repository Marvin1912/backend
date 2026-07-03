package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingCategoryRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingItemRequest;
import com.marvin.nutrition.entity.MealPlanShoppingCategoryEntity;
import com.marvin.nutrition.entity.MealPlanShoppingItemEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanShoppingCategoryRepository;
import com.marvin.nutrition.repository.MealPlanShoppingItemRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional write operations for meal-plan shopping-list categories and items.
 *
 * <p>All methods in this service perform blocking repository access and must only be called from
 * a thread already running on a blocking-friendly scheduler (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}),
 * and must be invoked from outside this bean so that Spring's transactional proxy is applied.</p>
 */
@Service
public class MealPlanShoppingListWriteService {

    private final MealPlanShoppingCategoryRepository mealPlanShoppingCategoryRepository;
    private final MealPlanShoppingItemRepository mealPlanShoppingItemRepository;
    private final MealPlanMapper mealPlanMapper;

    /**
     * Creates a new MealPlanShoppingListWriteService with the required dependencies.
     *
     * @param mealPlanShoppingCategoryRepository JPA repository for shopping categories
     * @param mealPlanShoppingItemRepository     JPA repository for shopping items
     * @param mealPlanMapper                     MapStruct mapper for entity/DTO conversion
     */
    public MealPlanShoppingListWriteService(
            MealPlanShoppingCategoryRepository mealPlanShoppingCategoryRepository,
            MealPlanShoppingItemRepository mealPlanShoppingItemRepository,
            MealPlanMapper mealPlanMapper) {
        this.mealPlanShoppingCategoryRepository = mealPlanShoppingCategoryRepository;
        this.mealPlanShoppingItemRepository = mealPlanShoppingItemRepository;
        this.mealPlanMapper = mealPlanMapper;
    }

    /**
     * Updates an existing shopping category's title.
     * Only non-null fields from the request are applied. The returned DTO includes the category's
     * current (unmodified) items.
     * Throws {@link NoSuchElementException} if no category with the given id exists.
     *
     * @param id  the UUID of the category to update
     * @param req the update request
     * @return the updated category DTO, including its items
     */
    @Transactional
    public MealPlanShoppingCategoryDTO updateShoppingCategory(UUID id, UpdateMealPlanShoppingCategoryRequest req) {
        final MealPlanShoppingCategoryEntity category = mealPlanShoppingCategoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan shopping category not found: " + id));

        if (req.title() != null) {
            category.setTitle(req.title());
        }

        final MealPlanShoppingCategoryEntity saved = mealPlanShoppingCategoryRepository.save(category);
        final List<MealPlanShoppingItemEntity> itemEntities = mealPlanShoppingItemRepository
                .findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(saved.getId());
        final List<MealPlanShoppingItemDTO> items = mealPlanMapper.toShoppingItemDTOs(itemEntities);
        return mealPlanMapper.toShoppingCategoryDTO(saved, items);
    }

    /**
     * Updates an existing shopping item's name, brand, badge, badge text and/or quantity.
     * Only non-null fields from the request are applied.
     * Throws {@link NoSuchElementException} if no item with the given id exists.
     *
     * @param id  the UUID of the item to update
     * @param req the update request
     * @return the updated item DTO
     */
    @Transactional
    public MealPlanShoppingItemDTO updateShoppingItem(UUID id, UpdateMealPlanShoppingItemRequest req) {
        final MealPlanShoppingItemEntity item = mealPlanShoppingItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan shopping item not found: " + id));

        if (req.name() != null) {
            item.setName(req.name());
        }
        if (req.brand() != null) {
            item.setBrand(req.brand());
        }
        if (req.badge() != null) {
            item.setBadge(req.badge());
        }
        if (req.badgeText() != null) {
            item.setBadgeText(req.badgeText());
        }
        if (req.qty() != null) {
            item.setQty(req.qty());
        }

        final MealPlanShoppingItemEntity saved = mealPlanShoppingItemRepository.save(item);
        return mealPlanMapper.toShoppingItemDTO(saved);
    }
}
