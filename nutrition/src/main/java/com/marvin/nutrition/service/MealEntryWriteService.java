package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateMealEntryRequest;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.UpdateMealEntryRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealEntryEntity;
import com.marvin.nutrition.mapper.MealEntryMapper;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional write operations for meal entries.
 *
 * <p>All methods in this service perform blocking repository access and must only be called from
 * a thread already running on a blocking-friendly scheduler (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}),
 * and must be invoked from outside this bean so that Spring's transactional proxy is applied.</p>
 */
@Service
public class MealEntryWriteService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MealEntryRepository mealEntryRepository;
    private final FoodRepository foodRepository;
    private final MealEntryMapper mealEntryMapper;
    private final DayTargetSnapshotService dayTargetSnapshotService;

    /**
     * Creates a new MealEntryWriteService with the required dependencies.
     *
     * @param mealEntryRepository      JPA repository for meal entries
     * @param foodRepository           JPA repository for food catalog entries
     * @param mealEntryMapper          MapStruct mapper for entity/DTO conversion
     * @param dayTargetSnapshotService service for creating per-day nutrition target snapshots
     */
    public MealEntryWriteService(
            MealEntryRepository mealEntryRepository,
            FoodRepository foodRepository,
            MealEntryMapper mealEntryMapper,
            DayTargetSnapshotService dayTargetSnapshotService) {
        this.mealEntryRepository = mealEntryRepository;
        this.foodRepository = foodRepository;
        this.mealEntryMapper = mealEntryMapper;
        this.dayTargetSnapshotService = dayTargetSnapshotService;
    }

    /**
     * Logs a new meal entry for the given date and ensures a day-target snapshot exists for it.
     * If {@code req.foodId()} is non-null, macros are snapshotted from the food catalog using
     * the supplied {@code quantityG}. Otherwise the entry is treated as ad-hoc and the supplied
     * macro values are persisted directly.
     * Throws {@link NoSuchElementException} if the referenced food is not found.
     * Throws {@link IllegalArgumentException} if required fields are missing for the chosen entry mode.
     *
     * @param date the date to log the entry for
     * @param req  the create request containing entry details
     * @return the created meal entry DTO
     */
    @Transactional
    public MealEntryDTO createEntry(LocalDate date, CreateMealEntryRequest req) {
        final MealEntryEntity entity = new MealEntryEntity();
        entity.setEntryDate(date);
        entity.setMealType(req.mealType());

        final MealEntryDTO dto;
        if (req.foodId() != null) {
            final String foodName = buildFoodEntry(entity, req);
            dto = mealEntryMapper.toDTO(mealEntryRepository.save(entity), foodName);
        } else {
            buildAdHocEntry(entity, req);
            dto = mealEntryMapper.toDTO(mealEntryRepository.save(entity));
        }

        dayTargetSnapshotService.ensureSnapshot(date);
        return dto;
    }

    /**
     * Logs multiple new meal entries for the given date in a single transaction and ensures a
     * day-target snapshot exists for it. Each request is processed the same way as
     * {@link #createEntry(LocalDate, CreateMealEntryRequest)}: food-backed entries (non-null
     * {@code foodId}) have their macros snapshotted from the food catalog, ad-hoc entries persist
     * the supplied macro values directly. The day-target snapshot is ensured exactly once after
     * all entries have been saved.
     * Throws {@link NoSuchElementException} if any referenced food is not found, rolling back the
     * whole batch.
     * Throws {@link IllegalArgumentException} if required fields are missing for any entry's mode.
     *
     * @param date     the date to log the entries for
     * @param requests the create requests, one per entry to be logged
     * @return the created meal entry DTOs, in the same order as {@code requests}
     */
    @Transactional
    public List<MealEntryDTO> createEntries(LocalDate date, List<CreateMealEntryRequest> requests) {
        final List<MealEntryDTO> created = new ArrayList<>();
        for (final CreateMealEntryRequest req : requests) {
            final MealEntryEntity entity = new MealEntryEntity();
            entity.setEntryDate(date);
            entity.setMealType(req.mealType());

            if (req.foodId() != null) {
                final String foodName = buildFoodEntry(entity, req);
                created.add(mealEntryMapper.toDTO(mealEntryRepository.save(entity), foodName));
            } else {
                buildAdHocEntry(entity, req);
                created.add(mealEntryMapper.toDTO(mealEntryRepository.save(entity)));
            }
        }

        dayTargetSnapshotService.ensureSnapshot(date);
        return created;
    }

    /**
     * Updates an existing meal entry.
     * For food-backed entries, a new {@code quantityG} triggers macro re-snapshotting from the food catalog.
     * For ad-hoc entries, any non-null macro values and description from the request are applied directly;
     * {@code quantityG} is ignored for ad-hoc entries because there is no food catalog item to re-snapshot from.
     * Throws {@link NoSuchElementException} if no entry with the given id exists.
     *
     * @param id  the UUID of the entry to update
     * @param req the update request
     * @return the updated meal entry DTO
     */
    @Transactional
    public MealEntryDTO updateEntry(UUID id, UpdateMealEntryRequest req) {
        final MealEntryEntity entity = mealEntryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal entry not found: " + id));

        if (req.mealType() != null) {
            entity.setMealType(req.mealType());
        }

        if (entity.getFoodId() != null) {
            final String foodName = applyFoodEntryUpdate(entity, req);
            return mealEntryMapper.toDTO(mealEntryRepository.save(entity), foodName);
        }

        applyAdHocEntryUpdate(entity, req);
        return mealEntryMapper.toDTO(mealEntryRepository.save(entity));
    }

    /**
     * Deletes the meal entry with the given id.
     * Throws {@link NoSuchElementException} if no entry with that id exists.
     *
     * @param id the UUID of the entry to delete
     */
    @Transactional
    public void deleteEntry(UUID id) {
        final MealEntryEntity entity = mealEntryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal entry not found: " + id));
        mealEntryRepository.delete(entity);
    }

    /**
     * Populates the entity fields for a food-backed entry by looking up the food and snapshotting macros.
     *
     * @param entity the entity to populate
     * @param req    the create request
     * @return the name of the referenced food item
     */
    private String buildFoodEntry(MealEntryEntity entity, CreateMealEntryRequest req) {
        final FoodEntity food = foodRepository.findById(req.foodId())
                .orElseThrow(() -> new NoSuchElementException("Food not found: " + req.foodId()));

        if (req.quantityG() == null) {
            throw new IllegalArgumentException("quantityG is required for a food entry");
        }

        entity.setFoodId(req.foodId());
        entity.setQuantityG(req.quantityG());
        entity.setDescription(req.description());
        entity.setKcal(snapshot(food.getKcalPer100(), req.quantityG()));
        entity.setProteinG(snapshot(food.getProteinPer100(), req.quantityG()));
        entity.setCarbsG(snapshot(food.getCarbsPer100(), req.quantityG()));
        entity.setFatG(snapshot(food.getFatPer100(), req.quantityG()));
        entity.setFoodName(food.getName());
        return food.getName();
    }

    /**
     * Populates the entity fields for an ad-hoc entry from the request.
     *
     * @param entity the entity to populate
     * @param req    the create request
     */
    private void buildAdHocEntry(MealEntryEntity entity, CreateMealEntryRequest req) {
        if (req.description() == null || req.description().isBlank()
                || req.kcal() == null || req.proteinG() == null
                || req.carbsG() == null || req.fatG() == null) {
            throw new IllegalArgumentException(
                    "ad-hoc entry requires description and kcal/proteinG/carbsG/fatG");
        }
        entity.setDescription(req.description());
        entity.setKcal(req.kcal());
        entity.setProteinG(req.proteinG());
        entity.setCarbsG(req.carbsG());
        entity.setFatG(req.fatG());
    }

    /**
     * Applies update fields for a food-backed entry, re-snapshotting macros if quantity changed.
     * Always resolves and returns the food name so the caller can populate {@code foodName} on the DTO.
     *
     * @param entity the entity to update
     * @param req    the update request
     * @return the name of the referenced food item
     */
    private String applyFoodEntryUpdate(MealEntryEntity entity, UpdateMealEntryRequest req) {
        final FoodEntity food = foodRepository.findById(entity.getFoodId())
                .orElseThrow(() -> new NoSuchElementException("Food not found: " + entity.getFoodId()));
        if (req.quantityG() != null) {
            entity.setQuantityG(req.quantityG());
            entity.setKcal(snapshot(food.getKcalPer100(), req.quantityG()));
            entity.setProteinG(snapshot(food.getProteinPer100(), req.quantityG()));
            entity.setCarbsG(snapshot(food.getCarbsPer100(), req.quantityG()));
            entity.setFatG(snapshot(food.getFatPer100(), req.quantityG()));
        }
        entity.setFoodName(food.getName());
        return food.getName();
    }

    /**
     * Applies update fields for an ad-hoc entry.
     * Note: {@code req.quantityG()} is intentionally ignored here — it is only meaningful for
     * food-backed entries where it drives macro re-snapshotting from the food catalog.
     *
     * @param entity the entity to update
     * @param req    the update request
     */
    private void applyAdHocEntryUpdate(MealEntryEntity entity, UpdateMealEntryRequest req) {
        if (req.description() != null) {
            entity.setDescription(req.description());
        }
        if (req.kcal() != null) {
            entity.setKcal(req.kcal());
        }
        if (req.proteinG() != null) {
            entity.setProteinG(req.proteinG());
        }
        if (req.carbsG() != null) {
            entity.setCarbsG(req.carbsG());
        }
        if (req.fatG() != null) {
            entity.setFatG(req.fatG());
        }
    }

    /**
     * Computes the snapshotted macro value: {@code per100 × quantityG / 100}, rounded half-up to 2 decimals.
     *
     * @param per100    the per-100-g value from the food catalog
     * @param quantityG the portion size in grams
     * @return the snapshotted value
     */
    private BigDecimal snapshot(BigDecimal per100, BigDecimal quantityG) {
        return per100.multiply(quantityG).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
