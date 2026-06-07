package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateWeightEntryRequest;
import com.marvin.nutrition.dto.WeightEntryDTO;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.mapper.WeightEntryMapper;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates CRUD operations for body-weight entries. */
@Service
public class WeightService {

    private final WeightEntryRepository weightEntryRepository;
    private final WeightEntryMapper weightEntryMapper;

    /**
     * Creates a new WeightService.
     *
     * @param weightEntryRepository JPA repository for weight entries
     * @param weightEntryMapper     MapStruct mapper for entity/DTO conversion
     */
    public WeightService(WeightEntryRepository weightEntryRepository, WeightEntryMapper weightEntryMapper) {
        this.weightEntryRepository = weightEntryRepository;
        this.weightEntryMapper = weightEntryMapper;
    }

    /**
     * Returns all weight entries ordered by entry date descending.
     *
     * @return a Flux emitting all weight entry DTOs
     */
    public Flux<WeightEntryDTO> findAll() {
        return Mono.fromCallable(() -> {
            final List<WeightEntryEntity> entries = weightEntryRepository.findAllByOrderByEntryDateDesc();
            return entries.stream().map(weightEntryMapper::toDTO).toList();
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(list -> list);
    }

    /**
     * Creates a new weight entry.
     *
     * @param request the entry date and weight in kg
     * @return a Mono emitting the created weight entry DTO
     */
    public Mono<WeightEntryDTO> create(CreateWeightEntryRequest request) {
        return Mono.fromCallable(() -> {
            final WeightEntryEntity entity = new WeightEntryEntity();
            entity.setEntryDate(request.entryDate());
            entity.setWeightKg(request.weightKg());
            return weightEntryMapper.toDTO(weightEntryRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates an existing weight entry.
     * Emits {@link NoSuchElementException} if no entry with the given id exists.
     *
     * @param id      the id of the entry to update
     * @param request the new entry date and weight
     * @return a Mono emitting the updated weight entry DTO
     */
    public Mono<WeightEntryDTO> update(Long id, CreateWeightEntryRequest request) {
        return Mono.fromCallable(() -> {
            final WeightEntryEntity entity = weightEntryRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Weight entry not found: " + id));
            entity.setEntryDate(request.entryDate());
            entity.setWeightKg(request.weightKg());
            return weightEntryMapper.toDTO(weightEntryRepository.save(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes the weight entry with the given id.
     * Emits {@link NoSuchElementException} if no entry with the given id exists.
     *
     * @param id the id of the entry to delete
     * @return an empty Mono on success
     */
    public Mono<Void> delete(Long id) {
        return Mono.fromCallable(() -> {
            if (!weightEntryRepository.existsById(id)) {
                throw new NoSuchElementException("Weight entry not found: " + id);
            }
            weightEntryRepository.deleteById(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
