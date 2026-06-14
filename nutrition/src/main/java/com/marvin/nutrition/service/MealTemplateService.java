package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateMealTemplateRequest;
import com.marvin.nutrition.dto.MealTemplateDTO;
import com.marvin.nutrition.dto.MealTemplateItemDTO;
import com.marvin.nutrition.dto.UpdateMealTemplateRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealTemplateEntity;
import com.marvin.nutrition.entity.MealTemplateItemEntity;
import com.marvin.nutrition.mapper.MealTemplateMapper;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealTemplateItemRepository;
import com.marvin.nutrition.repository.MealTemplateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates CRUD operations for meal templates, including live macro computation for their items. */
@Service
public class MealTemplateService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MealTemplateRepository mealTemplateRepository;
    private final MealTemplateItemRepository mealTemplateItemRepository;
    private final FoodRepository foodRepository;
    private final MealTemplateMapper mealTemplateMapper;
    private final MealTemplateWriteService mealTemplateWriteService;

    /**
     * Creates a new MealTemplateService with the required dependencies.
     *
     * @param mealTemplateRepository     JPA repository for meal templates
     * @param mealTemplateItemRepository JPA repository for meal template items
     * @param foodRepository             JPA repository for food catalog entries
     * @param mealTemplateMapper         MapStruct mapper for entity/DTO conversion
     * @param mealTemplateWriteService   service owning transactional meal template write operations
     */
    public MealTemplateService(
            MealTemplateRepository mealTemplateRepository,
            MealTemplateItemRepository mealTemplateItemRepository,
            FoodRepository foodRepository,
            MealTemplateMapper mealTemplateMapper,
            MealTemplateWriteService mealTemplateWriteService) {
        this.mealTemplateRepository = mealTemplateRepository;
        this.mealTemplateItemRepository = mealTemplateItemRepository;
        this.foodRepository = foodRepository;
        this.mealTemplateMapper = mealTemplateMapper;
        this.mealTemplateWriteService = mealTemplateWriteService;
    }

    /**
     * Returns all meal templates ordered alphabetically by name, with each item's macros
     * live-computed from the current food catalog.
     *
     * @return a Mono emitting the list of meal template DTOs
     */
    public Mono<List<MealTemplateDTO>> findAll() {
        return Mono.fromCallable(() -> {
            final List<MealTemplateEntity> templates = mealTemplateRepository.findAllByOrderByNameAsc();
            final Map<UUID, List<MealTemplateItemEntity>> itemsByTemplateId = templates.stream()
                    .collect(Collectors.toMap(MealTemplateEntity::getId,
                            t -> mealTemplateItemRepository.findByMealTemplateId(t.getId())));
            final List<MealTemplateItemEntity> allItems = itemsByTemplateId.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            final Map<UUID, FoodEntity> foodById = loadFoodsByIds(allItems);

            return templates.stream()
                    .map(template -> toDTO(template, itemsByTemplateId.get(template.getId()), foodById))
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Returns the meal template with the given id, with each item's macros live-computed from the
     * current food catalog.
     * Emits {@link NoSuchElementException} if no template with that id exists.
     *
     * @param id the UUID of the meal template
     * @return a Mono emitting the meal template DTO
     */
    public Mono<MealTemplateDTO> findById(UUID id) {
        return Mono.fromCallable(() -> {
            final MealTemplateEntity template = mealTemplateRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Meal template not found: " + id));
            final List<MealTemplateItemEntity> items = mealTemplateItemRepository.findByMealTemplateId(id);
            final Map<UUID, FoodEntity> foodById = loadFoodsByIds(items);
            return toDTO(template, items, foodById);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates a new meal template.
     * Emits {@link NoSuchElementException} if any referenced food is not found.
     *
     * @param req the create request containing the template name and items
     * @return a Mono emitting the created meal template DTO
     */
    public Mono<MealTemplateDTO> create(CreateMealTemplateRequest req) {
        return Mono.fromCallable(() -> mealTemplateWriteService.create(req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::toDTO);
    }

    /**
     * Replaces the name and entire item composition of an existing meal template.
     * Emits {@link NoSuchElementException} if the template does not exist or if any referenced
     * food is not found.
     *
     * @param id  the UUID of the template to update
     * @param req the update request containing the new name and the replacement items
     * @return a Mono emitting the updated meal template DTO
     */
    public Mono<MealTemplateDTO> update(UUID id, UpdateMealTemplateRequest req) {
        return Mono.fromCallable(() -> mealTemplateWriteService.update(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::toDTO);
    }

    /**
     * Deletes the meal template with the given id.
     * Emits {@link NoSuchElementException} if no template with that id exists.
     *
     * @param id the UUID of the template to delete
     * @return an empty Mono on success
     */
    public Mono<Void> delete(UUID id) {
        return Mono.fromCallable(() -> {
            mealTemplateWriteService.delete(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Assembles a {@link MealTemplateDTO} from a write-service result, resolving food names and
     * macros for the saved items.
     *
     * @param writeResult the saved template together with its saved items
     * @return the assembled meal template DTO
     */
    private MealTemplateDTO toDTO(MealTemplateWriteService.MealTemplateWithItems writeResult) {
        final Map<UUID, FoodEntity> foodById = loadFoodsByIds(writeResult.items());
        return toDTO(writeResult.template(), writeResult.items(), foodById);
    }

    /**
     * Assembles a {@link MealTemplateDTO} from a template entity and its items, resolving food
     * names and live-computed macros via the given food lookup map.
     *
     * @param template the meal template entity
     * @param items    the template's item entities
     * @param foodById foods referenced by the items, keyed by id
     * @return the assembled meal template DTO
     */
    private MealTemplateDTO toDTO(MealTemplateEntity template, List<MealTemplateItemEntity> items, Map<UUID, FoodEntity> foodById) {
        final List<MealTemplateItemDTO> itemDTOs = items.stream()
                .map(item -> toItemDTO(item, foodById.get(item.getFoodId())))
                .collect(Collectors.toList());
        return new MealTemplateDTO(template.getId(), template.getName(), itemDTOs);
    }

    /**
     * Maps a single template item to its DTO, live-computing macros from the referenced food.
     *
     * @param item the template item entity
     * @param food the referenced food entity
     * @return the mapped item DTO with live-computed macros
     */
    private MealTemplateItemDTO toItemDTO(MealTemplateItemEntity item, FoodEntity food) {
        return mealTemplateMapper.toItemDTO(
                item,
                food.getName(),
                snapshot(food.getKcalPer100(), item.getQuantityG()),
                snapshot(food.getProteinPer100(), item.getQuantityG()),
                snapshot(food.getCarbsPer100(), item.getQuantityG()),
                snapshot(food.getFatPer100(), item.getQuantityG())
        );
    }

    /**
     * Loads all foods referenced by the given items in a single query, keyed by id.
     *
     * @param items the template items whose referenced foods should be loaded
     * @return a map of food id to food entity
     */
    private Map<UUID, FoodEntity> loadFoodsByIds(List<MealTemplateItemEntity> items) {
        final List<UUID> foodIds = items.stream()
                .map(MealTemplateItemEntity::getFoodId)
                .distinct()
                .collect(Collectors.toList());
        return foodRepository.findAllById(foodIds).stream()
                .collect(Collectors.toMap(FoodEntity::getId, food -> food));
    }

    /**
     * Computes the live macro value: {@code per100 × quantityG / 100}, rounded half-up to 2 decimals.
     *
     * @param per100    the per-100-g value from the food catalog
     * @param quantityG the portion size in grams
     * @return the computed value
     */
    private BigDecimal snapshot(BigDecimal per100, BigDecimal quantityG) {
        return per100.multiply(quantityG).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
