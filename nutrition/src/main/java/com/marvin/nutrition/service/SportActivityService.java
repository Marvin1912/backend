package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateSportActivityRequest;
import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.dto.UpdateSportActivityRequest;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates sport activity logging operations, delegating writes to {@link SportActivityWriteService}. */
@Service
public class SportActivityService {

    private final SportActivityWriteService sportActivityWriteService;

    /**
     * Creates a new SportActivityService with the required dependencies.
     *
     * @param sportActivityWriteService service owning transactional sport activity write operations
     */
    public SportActivityService(SportActivityWriteService sportActivityWriteService) {
        this.sportActivityWriteService = sportActivityWriteService;
    }

    /**
     * Logs a new sport activity for the given date.
     * Emits {@link IllegalArgumentException} if {@code req.activityType()} is OTHER and
     * {@code req.description()} is null or blank.
     *
     * @param date the date to log the activity for
     * @param req  the create request containing activity details
     * @return a Mono emitting the created sport activity DTO
     */
    public Mono<SportActivityDTO> addActivity(LocalDate date, CreateSportActivityRequest req) {
        return Mono.fromCallable(() -> sportActivityWriteService.createActivity(date, req))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates an existing sport activity.
     * Emits {@link NoSuchElementException} if no activity with the given id exists.
     * Emits {@link IllegalArgumentException} if the resulting activity type is OTHER and the
     * resulting description is null or blank.
     *
     * @param id  the UUID of the activity to update
     * @param req the update request
     * @return a Mono emitting the updated sport activity DTO
     */
    public Mono<SportActivityDTO> updateActivity(UUID id, UpdateSportActivityRequest req) {
        return Mono.fromCallable(() -> sportActivityWriteService.updateActivity(id, req))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes the sport activity with the given id.
     * Emits {@link NoSuchElementException} if no activity with that id exists.
     *
     * @param id the UUID of the activity to delete
     * @return an empty Mono on success
     */
    public Mono<Void> deleteActivity(UUID id) {
        return Mono.fromCallable(() -> {
            sportActivityWriteService.deleteActivity(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
