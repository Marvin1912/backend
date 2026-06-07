package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.mapper.FoodMapper;
import com.marvin.nutrition.repository.FoodRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates CRUD and search operations for the food catalog. */
@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final FoodMapper foodMapper;

    /**
     * Creates a new FoodService with the required dependencies.
     *
     * @param foodRepository JPA repository for food entries
     * @param foodMapper     MapStruct mapper for entity/DTO conversion
     */
    public FoodService(FoodRepository foodRepository, FoodMapper foodMapper) {
        this.foodRepository = foodRepository;
        this.foodMapper = foodMapper;
    }

    /**
     * Returns all food entries, or those matching the given name query.
     * If {@code q} is null or blank, all foods ordered by name are returned.
     * Otherwise, a case-insensitive name-contains search is performed.
     *
     * @param q optional search string to filter by name
     * @return a Flux emitting matching food DTOs
     */
    public Flux<FoodDTO> findAll(String q) {
        return Mono.fromCallable(() -> {
            final List<FoodEntity> entities;
            if (q == null || q.isBlank()) {
                entities = foodRepository.findAll(
                        org.springframework.data.domain.Sort.by("name"));
            } else {
                entities = foodRepository.searchByName(q);
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
            if (entity.getSource() == null) {
                entity.setSource(FoodSource.MANUAL);
            }
            return foodMapper.toDTO(foodRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates an existing food entry.
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
            if (dto.source() != null) {
                entity.setSource(dto.source());
            }
            return foodMapper.toDTO(foodRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes the food entry with the given id.
     * Emits {@link NoSuchElementException} if no entry with that id exists.
     *
     * @param id the UUID of the food entry to delete
     * @return an empty Mono on success
     */
    public Mono<Void> delete(UUID id) {
        return Mono.fromCallable(() -> {
            if (!foodRepository.existsById(id)) {
                throw new NoSuchElementException("Food not found: " + id);
            }
            foodRepository.deleteById(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
