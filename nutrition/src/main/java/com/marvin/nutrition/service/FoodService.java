package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.mapper.FoodMapper;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealTemplateItemRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates CRUD and search operations for the food catalog. */
@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final FoodMapper foodMapper;
    private final MealPlanRowRepository mealPlanRowRepository;
    private final MealTemplateItemRepository mealTemplateItemRepository;

    /**
     * Creates a new FoodService with the required dependencies.
     *
     * @param foodRepository             JPA repository for food entries
     * @param foodMapper                 MapStruct mapper for entity/DTO conversion
     * @param mealPlanRowRepository      JPA repository for meal-plan rows, used to guard food deletion
     * @param mealTemplateItemRepository JPA repository for meal-template items, used to guard food deletion
     */
    public FoodService(
            FoodRepository foodRepository,
            FoodMapper foodMapper,
            MealPlanRowRepository mealPlanRowRepository,
            MealTemplateItemRepository mealTemplateItemRepository) {
        this.foodRepository = foodRepository;
        this.foodMapper = foodMapper;
        this.mealPlanRowRepository = mealPlanRowRepository;
        this.mealTemplateItemRepository = mealTemplateItemRepository;
    }

    /**
     * Returns a page of food entries, or those matching the given name query.
     * If {@code q} is null or blank, all foods ordered by name are returned, paginated.
     * Otherwise, a case-insensitive name-contains search is performed, paginated.
     * LIKE special characters ({@code \}, {@code %}, {@code _}) in the query are
     * escaped before being passed to the repository.
     *
     * @param q    optional search string to filter by name
     * @param page zero-based page number
     * @param size page size
     * @return a Flux emitting matching food DTOs for the requested page
     */
    public Flux<FoodDTO> findAll(String q, int page, int size) {
        return Mono.fromCallable(() -> {
            final List<FoodEntity> entities;
            if (q == null || q.isBlank()) {
                final Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
                entities = foodRepository.findAll(pageable).getContent();
            } else {
                entities = foodRepository.searchByName(escapeLike(q), PageRequest.of(page, size));
            }
            return foodMapper.toDTOList(entities);
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(list -> list);
    }

    /**
     * Returns the food entry with the given id.
     * Emits {@link NoSuchElementException} if no entry with that id exists.
     *
     * @param id the UUID of the food entry
     * @return a Mono emitting the food DTO
     */
    public Mono<FoodDTO> findById(UUID id) {
        return Mono.fromCallable(() -> {
            final FoodEntity entity = foodRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Food not found: " + id));
            return foodMapper.toDTO(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates a new food entry. If no source is specified, defaults to {@link FoodSource#MANUAL}.
     *
     * @param dto the food data to persist
     * @return a Mono emitting the created food DTO
     */
    public Mono<FoodDTO> create(FoodDTO dto) {
        return Mono.fromCallable(() -> {
            final FoodEntity entity = foodMapper.toEntity(dto);
            entity.setSource(resolveSource(dto.source()));
            return foodMapper.toDTO(foodRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates an existing food entry. All fields are replaced unconditionally;
     * if {@code source} is null in the DTO it defaults to {@link FoodSource#MANUAL}.
     * Emits {@link NoSuchElementException} if no entry with that id exists.
     *
     * @param id  the UUID of the food entry to update
     * @param dto the updated food data
     * @return a Mono emitting the updated food DTO
     */
    public Mono<FoodDTO> update(UUID id, FoodDTO dto) {
        return Mono.fromCallable(() -> {
            final FoodEntity entity = foodRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Food not found: " + id));
            entity.setName(dto.name());
            entity.setBrand(dto.brand());
            entity.setKcalPer100(dto.kcalPer100());
            entity.setProteinPer100(dto.proteinPer100());
            entity.setCarbsPer100(dto.carbsPer100());
            entity.setFatPer100(dto.fatPer100());
            entity.setFiberPer100(dto.fiberPer100());
            entity.setDefaultServingG(dto.defaultServingG());
            entity.setSource(resolveSource(dto.source()));
            return foodMapper.toDTO(foodRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes the food entry with the given id.
     * Emits {@link NoSuchElementException} if no entry with that id exists.
     * Emits {@link FoodReferencedException} if the food is still referenced by any meal-plan row
     * or meal-template item, carrying the reference counts for the caller to report.
     *
     * @param id the UUID of the food entry to delete
     * @return an empty Mono on success
     */
    public Mono<Void> delete(UUID id) {
        return Mono.fromCallable(() -> {
            if (!foodRepository.existsById(id)) {
                throw new NoSuchElementException("Food not found: " + id);
            }
            final long mealPlanRowCount = mealPlanRowRepository.countByFoodId(id);
            final long mealTemplateItemCount = mealTemplateItemRepository.countByFoodId(id);
            if (mealPlanRowCount > 0 || mealTemplateItemCount > 0) {
                throw new FoodReferencedException(mealPlanRowCount, mealTemplateItemCount);
            }
            foodRepository.deleteById(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Returns the given source if non-null, otherwise {@link FoodSource#MANUAL}.
     *
     * @param source the source value from the DTO, may be null
     * @return a non-null FoodSource
     */
    private FoodSource resolveSource(FoodSource source) {
        return source != null ? source : FoodSource.MANUAL;
    }

    /**
     * Escapes LIKE special characters ({@code \}, {@code %}, {@code _}) in the given string
     * so that they are matched literally by the repository query.
     * Backslash is escaped first to avoid double-escaping.
     *
     * @param q the raw user query
     * @return the escaped query safe for use in a LIKE pattern with {@code ESCAPE '\\'}
     */
    private String escapeLike(String q) {
        return q.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
