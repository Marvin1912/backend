package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateMealTemplateRequest;
import com.marvin.nutrition.dto.MealTemplateItemRequest;
import com.marvin.nutrition.dto.SaveEstimateAsTemplateRequest;
import com.marvin.nutrition.dto.UpdateMealTemplateRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.entity.MealTemplateEntity;
import com.marvin.nutrition.entity.MealTemplateItemEntity;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealTemplateItemRepository;
import com.marvin.nutrition.repository.MealTemplateRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional write operations for meal templates.
 *
 * <p>All methods in this service perform blocking repository access and must only be called from
 * a thread already running on a blocking-friendly scheduler (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}),
 * and must be invoked from outside this bean so that Spring's transactional proxy is applied.</p>
 */
@Service
public class MealTemplateWriteService {

    private final MealTemplateRepository mealTemplateRepository;
    private final MealTemplateItemRepository mealTemplateItemRepository;
    private final FoodRepository foodRepository;

    /**
     * Creates a new MealTemplateWriteService with the required dependencies.
     *
     * @param mealTemplateRepository     JPA repository for meal templates
     * @param mealTemplateItemRepository JPA repository for meal template items
     * @param foodRepository             JPA repository for food catalog entries
     */
    public MealTemplateWriteService(
            MealTemplateRepository mealTemplateRepository,
            MealTemplateItemRepository mealTemplateItemRepository,
            FoodRepository foodRepository) {
        this.mealTemplateRepository = mealTemplateRepository;
        this.mealTemplateItemRepository = mealTemplateItemRepository;
        this.foodRepository = foodRepository;
    }

    /**
     * Creates a new meal template with the given name and items.
     * Every {@code foodId} referenced by an item must exist in the food catalog.
     * Throws {@link NoSuchElementException} if any referenced food is not found.
     *
     * @param req the create request containing the template name and items
     * @return the saved template together with its saved items
     */
    @Transactional
    public MealTemplateWithItems create(CreateMealTemplateRequest req) {
        validateFoodIds(req.items());

        final MealTemplateEntity template = new MealTemplateEntity();
        template.setName(req.name());
        final MealTemplateEntity savedTemplate = mealTemplateRepository.save(template);

        final List<MealTemplateItemEntity> savedItems = saveItems(savedTemplate.getId(), req.items());
        return new MealTemplateWithItems(savedTemplate, savedItems);
    }

    /**
     * Replaces the name and entire item composition of an existing meal template.
     * Every {@code foodId} referenced by an item must exist in the food catalog.
     * Throws {@link NoSuchElementException} if the template does not exist or if any referenced
     * food is not found.
     *
     * @param id  the UUID of the template to update
     * @param req the update request containing the new name and the replacement items
     * @return the updated template together with its newly saved items
     */
    @Transactional
    public MealTemplateWithItems update(UUID id, UpdateMealTemplateRequest req) {
        final MealTemplateEntity template = mealTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal template not found: " + id));

        validateFoodIds(req.items());

        template.setName(req.name());
        final MealTemplateEntity savedTemplate = mealTemplateRepository.save(template);

        mealTemplateItemRepository.deleteByMealTemplateId(id);
        final List<MealTemplateItemEntity> savedItems = saveItems(id, req.items());
        return new MealTemplateWithItems(savedTemplate, savedItems);
    }

    /**
     * Deletes the meal template with the given id. Deleting the template cascades to remove its items.
     * Throws {@link NoSuchElementException} if no template with that id exists.
     *
     * @param id the UUID of the template to delete
     */
    @Transactional
    public void delete(UUID id) {
        final MealTemplateEntity template = mealTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal template not found: " + id));
        mealTemplateRepository.delete(template);
    }

    /**
     * Atomically creates a synthetic {@link FoodEntity} from the given estimate, then creates a
     * {@link MealTemplateEntity} with that food as its sole item at 100 g.
     *
     * <p>The food entry is created with {@link FoodSource#ESTIMATE}, all per-100-g macro fields set
     * to the estimate values, a fixed {@code defaultServingG} of 100, and no brand.</p>
     *
     * @param req the estimate request containing the template name and macro values
     * @return the saved template together with its single saved item
     */
    @Transactional
    public MealTemplateWithItems createFromEstimate(SaveEstimateAsTemplateRequest req) {
        final FoodEntity food = new FoodEntity();
        food.setName(req.name());
        food.setBrand(null);
        food.setSource(FoodSource.ESTIMATE);
        food.setKcalPer100(req.kcal());
        food.setProteinPer100(req.proteinG());
        food.setCarbsPer100(req.carbsG());
        food.setFatPer100(req.fatG());
        food.setDefaultServingG(BigDecimal.valueOf(100));
        final FoodEntity savedFood = foodRepository.save(food);

        final MealTemplateEntity template = new MealTemplateEntity();
        template.setName(req.name());
        final MealTemplateEntity savedTemplate = mealTemplateRepository.save(template);

        final MealTemplateItemEntity item = new MealTemplateItemEntity();
        item.setMealTemplateId(savedTemplate.getId());
        item.setFoodId(savedFood.getId());
        item.setQuantityG(BigDecimal.valueOf(100));
        final MealTemplateItemEntity savedItem = mealTemplateItemRepository.save(item);

        return new MealTemplateWithItems(savedTemplate, List.of(savedItem));
    }

    /**
     * Verifies that every {@code foodId} referenced by the given items exists in the food catalog.
     *
     * @param items the requested template items
     * @throws NoSuchElementException if any referenced food is not found
     */
    private void validateFoodIds(List<MealTemplateItemRequest> items) {
        final List<UUID> requestedFoodIds = items.stream()
                .map(MealTemplateItemRequest::foodId)
                .distinct()
                .collect(Collectors.toList());

        final List<UUID> existingFoodIds = foodRepository.findAllById(requestedFoodIds).stream()
                .map(FoodEntity::getId)
                .collect(Collectors.toList());

        for (final UUID foodId : requestedFoodIds) {
            if (!existingFoodIds.contains(foodId)) {
                throw new NoSuchElementException("Food not found: " + foodId);
            }
        }
    }

    /**
     * Saves a {@link MealTemplateItemEntity} for each requested item, linked to the given template.
     *
     * @param templateId the id of the owning meal template
     * @param items      the requested items to persist
     * @return the saved item entities, in the same order as {@code items}
     */
    private List<MealTemplateItemEntity> saveItems(UUID templateId, List<MealTemplateItemRequest> items) {
        final List<MealTemplateItemEntity> savedItems = new ArrayList<>();
        for (final MealTemplateItemRequest itemReq : items) {
            final MealTemplateItemEntity item = new MealTemplateItemEntity();
            item.setMealTemplateId(templateId);
            item.setFoodId(itemReq.foodId());
            item.setQuantityG(itemReq.quantityG());
            savedItems.add(mealTemplateItemRepository.save(item));
        }
        return savedItems;
    }

    /**
     * Holds a saved meal template together with its saved items, for assembly into a response DTO
     * by the reactive {@link MealTemplateService}.
     *
     * @param template the saved meal template entity
     * @param items    the saved meal template item entities belonging to the template
     */
    public record MealTemplateWithItems(MealTemplateEntity template, List<MealTemplateItemEntity> items) {
    }
}
