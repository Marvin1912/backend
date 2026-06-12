package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateWeightEntryRequest;
import com.marvin.nutrition.dto.WeightEntryDTO;
import com.marvin.nutrition.entity.WeightEntryEntity;
import com.marvin.nutrition.mapper.WeightEntryMapper;
import com.marvin.nutrition.repository.WeightEntryRepository;
import java.time.LocalDate;
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
    private final DayTargetSnapshotService dayTargetSnapshotService;

    /**
     * Creates a new WeightService.
     *
     * @param weightEntryRepository    JPA repository for weight entries
     * @param weightEntryMapper        MapStruct mapper for entity/DTO conversion
     * @param dayTargetSnapshotService service for creating/refreshing per-day nutrition target snapshots
     */
    public WeightService(
            WeightEntryRepository weightEntryRepository,
            WeightEntryMapper weightEntryMapper,
            DayTargetSnapshotService dayTargetSnapshotService) {
        this.weightEntryRepository = weightEntryRepository;
        this.weightEntryMapper = weightEntryMapper;
        this.dayTargetSnapshotService = dayTargetSnapshotService;
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
     * Refreshes the day-target snapshot for the entry date if one already exists, so that the
     * newly recorded weight is reflected in that day's targets.
     *
     * @param request the entry date and weight in kg
     * @return a Mono emitting the created weight entry DTO
     */
    public Mono<WeightEntryDTO> create(CreateWeightEntryRequest request) {
        return Mono.fromCallable(() -> {
            final WeightEntryEntity entity = new WeightEntryEntity();
            entity.setEntryDate(request.entryDate());
            entity.setWeightKg(request.weightKg());
            final WeightEntryDTO dto = weightEntryMapper.toDTO(weightEntryRepository.save(entity));
            dayTargetSnapshotService.refreshSnapshotIfExists(request.entryDate());
            return dto;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates an existing weight entry.
     * Refreshes the day-target snapshot for the new entry date if one already exists. If the entry
     * date changed, also refreshes the snapshot for the previous date, since the applicable weight
     * for that day may now differ.
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
            final LocalDate oldEntryDate = entity.getEntryDate();
            entity.setEntryDate(request.entryDate());
            entity.setWeightKg(request.weightKg());
            final WeightEntryDTO dto = weightEntryMapper.toDTO(weightEntryRepository.save(entity));
            dayTargetSnapshotService.refreshSnapshotIfExists(request.entryDate());
            if (!oldEntryDate.equals(request.entryDate())) {
                dayTargetSnapshotService.refreshSnapshotIfExists(oldEntryDate);
            }
            return dto;
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
