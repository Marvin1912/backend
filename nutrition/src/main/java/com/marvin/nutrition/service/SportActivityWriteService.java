package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateSportActivityRequest;
import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.dto.UpdateSportActivityRequest;
import com.marvin.nutrition.entity.SportActivityEntity;
import com.marvin.nutrition.entity.SportActivityType;
import com.marvin.nutrition.mapper.SportActivityMapper;
import com.marvin.nutrition.repository.SportActivityRepository;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional write operations for sport activities.
 *
 * <p>All methods in this service perform blocking repository access and must only be called from
 * a thread already running on a blocking-friendly scheduler (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}),
 * and must be invoked from outside this bean so that Spring's transactional proxy is applied.</p>
 */
@Service
public class SportActivityWriteService {

    private final SportActivityRepository sportActivityRepository;
    private final SportActivityMapper sportActivityMapper;

    /**
     * Creates a new SportActivityWriteService with the required dependencies.
     *
     * @param sportActivityRepository JPA repository for sport activities
     * @param sportActivityMapper     MapStruct mapper for entity/DTO conversion
     */
    public SportActivityWriteService(
            SportActivityRepository sportActivityRepository,
            SportActivityMapper sportActivityMapper) {
        this.sportActivityRepository = sportActivityRepository;
        this.sportActivityMapper = sportActivityMapper;
    }

    /**
     * Logs a new sport activity for the given date.
     * Throws {@link IllegalArgumentException} if {@code req.activityType()} is OTHER and
     * {@code req.description()} is null or blank.
     *
     * @param date the date to log the activity for
     * @param req  the create request containing activity details
     * @return the created sport activity DTO
     */
    @Transactional
    public SportActivityDTO createActivity(LocalDate date, CreateSportActivityRequest req) {
        requireDescriptionForOther(req.activityType(), req.description());

        final SportActivityEntity entity = new SportActivityEntity();
        entity.setEntryDate(date);
        entity.setActivityType(req.activityType());
        entity.setDescription(req.description());
        entity.setKcalBurned(req.kcalBurned());

        return sportActivityMapper.toDTO(sportActivityRepository.save(entity));
    }

    /**
     * Updates an existing sport activity.
     * Only non-null fields from the request are applied.
     * Throws {@link NoSuchElementException} if no activity with the given id exists.
     * Throws {@link IllegalArgumentException} if the resulting activity type is OTHER and the
     * resulting description is null or blank.
     *
     * @param id  the UUID of the activity to update
     * @param req the update request
     * @return the updated sport activity DTO
     */
    @Transactional
    public SportActivityDTO updateActivity(UUID id, UpdateSportActivityRequest req) {
        final SportActivityEntity entity = sportActivityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Sport activity not found: " + id));

        if (req.activityType() != null) {
            entity.setActivityType(req.activityType());
        }
        if (req.description() != null) {
            entity.setDescription(req.description());
        }
        if (req.kcalBurned() != null) {
            entity.setKcalBurned(req.kcalBurned());
        }

        requireDescriptionForOther(entity.getActivityType(), entity.getDescription());

        return sportActivityMapper.toDTO(sportActivityRepository.save(entity));
    }

    /**
     * Deletes the sport activity with the given id.
     * Throws {@link NoSuchElementException} if no activity with that id exists.
     *
     * @param id the UUID of the activity to delete
     */
    @Transactional
    public void deleteActivity(UUID id) {
        final SportActivityEntity entity = sportActivityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Sport activity not found: " + id));
        sportActivityRepository.delete(entity);
    }

    /**
     * Validates that a non-blank description is present when the activity type is OTHER.
     *
     * @param activityType the activity type to check
     * @param description  the description to check
     */
    private void requireDescriptionForOther(SportActivityType activityType, String description) {
        if (activityType == SportActivityType.OTHER && (description == null || description.isBlank())) {
            throw new IllegalArgumentException("description is required for OTHER activity type");
        }
    }
}
