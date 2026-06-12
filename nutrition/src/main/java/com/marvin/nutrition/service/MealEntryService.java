package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateMealEntryRequest;
import com.marvin.nutrition.dto.DaySummaryDTO;
import com.marvin.nutrition.dto.MacrosDTO;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.dto.UpdateMealEntryRequest;
import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealEntryEntity;
import com.marvin.nutrition.mapper.MealEntryMapper;
import com.marvin.nutrition.repository.DayTargetSnapshotRepository;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrates meal logging operations including CRUD and day summary computation. */
@Service
public class MealEntryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MealEntryRepository mealEntryRepository;
    private final FoodRepository foodRepository;
    private final MealEntryMapper mealEntryMapper;
    private final NutritionTargetService nutritionTargetService;
    private final DayTargetSnapshotRepository dayTargetSnapshotRepository;

    /**
     * Creates a new MealEntryService with the required dependencies.
     *
     * @param mealEntryRepository         JPA repository for meal entries
     * @param foodRepository              JPA repository for food catalog entries
     * @param mealEntryMapper             MapStruct mapper for entity/DTO conversion
     * @param nutritionTargetService      service for computing daily nutrition targets
     * @param dayTargetSnapshotRepository JPA repository for per-day nutrition target snapshots
     */
    public MealEntryService(
            MealEntryRepository mealEntryRepository,
            FoodRepository foodRepository,
            MealEntryMapper mealEntryMapper,
            NutritionTargetService nutritionTargetService,
            DayTargetSnapshotRepository dayTargetSnapshotRepository) {
        this.mealEntryRepository = mealEntryRepository;
        this.foodRepository = foodRepository;
        this.mealEntryMapper = mealEntryMapper;
        this.nutritionTargetService = nutritionTargetService;
        this.dayTargetSnapshotRepository = dayTargetSnapshotRepository;
    }

    /**
     * Logs a new meal entry for the given date.
     * If {@code req.foodId()} is non-null, macros are snapshotted from the food catalog using
     * the supplied {@code quantityG}. Otherwise the entry is treated as ad-hoc and the supplied
     * macro values are persisted directly.
     * Emits {@link NoSuchElementException} if the referenced food is not found.
     * Emits {@link IllegalArgumentException} if required fields are missing for the chosen entry mode.
     *
     * @param date the date to log the entry for
     * @param req  the create request containing entry details
     * @return a Mono emitting the created meal entry DTO
     */
    public Mono<MealEntryDTO> addEntry(LocalDate date, CreateMealEntryRequest req) {
        return Mono.fromCallable(() -> {
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

            ensureDayTargetSnapshot(date);
            return dto;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Persists a {@link DayTargetSnapshotEntity} for the given date if one does not already exist
     * and the daily nutrition targets can currently be computed. Silently does nothing if a
     * snapshot already exists or if targets cannot be computed yet (e.g. no profile/weight data).
     *
     * @param date the date to snapshot targets for
     */
    private void ensureDayTargetSnapshot(LocalDate date) {
        if (dayTargetSnapshotRepository.findById(date).isPresent()) {
            return;
        }
        final TargetsDTO targets;
        try {
            targets = nutritionTargetService.getTargets(date).block();
        } catch (TargetCalculationException e) {
            return;
        }
        if (targets == null) {
            return;
        }
        final DayTargetSnapshotEntity snapshot = new DayTargetSnapshotEntity();
        snapshot.setEntryDate(date);
        snapshot.setBmr(targets.bmr());
        snapshot.setMaintenanceKcal(targets.maintenanceKcal());
        snapshot.setTargetKcal(targets.targetKcal());
        snapshot.setProteinG(targets.proteinG());
        snapshot.setFatG(targets.fatG());
        snapshot.setCarbsG(targets.carbsG());
        snapshot.setBasis(targets.basis());
        dayTargetSnapshotRepository.save(snapshot);
    }

    /**
     * Updates an existing meal entry.
     * For food-backed entries, a new {@code quantityG} triggers macro re-snapshotting from the food catalog.
     * For ad-hoc entries, any non-null macro values and description from the request are applied directly;
     * {@code quantityG} is ignored for ad-hoc entries because there is no food catalog item to re-snapshot from.
     * Emits {@link NoSuchElementException} if no entry with the given id exists.
     *
     * @param id  the UUID of the entry to update
     * @param req the update request
     * @return a Mono emitting the updated meal entry DTO
     */
    public Mono<MealEntryDTO> updateEntry(UUID id, UpdateMealEntryRequest req) {
        return Mono.fromCallable(() -> {
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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes the meal entry with the given id.
     * Emits {@link NoSuchElementException} if no entry with that id exists.
     *
     * @param id the UUID of the entry to delete
     * @return an empty Mono on success
     */
    public Mono<Void> deleteEntry(UUID id) {
        return Mono.fromCallable(() -> {
            final MealEntryEntity entity = mealEntryRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Meal entry not found: " + id));
            mealEntryRepository.delete(entity);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Returns the day summary for the given date, including all entries, totals, targets and remaining macros.
     * If the nutrition target service cannot compute targets (e.g. missing profile/weight),
     * targets and remaining will be null.
     *
     * @param date the date to summarise
     * @return a Mono emitting the day summary DTO
     */
    public Mono<DaySummaryDTO> getDay(LocalDate date) {
        final Mono<List<MealEntryDTO>> entriesMono = Mono.fromCallable(() -> {
            final List<MealEntryEntity> entities =
                    mealEntryRepository.findByEntryDateOrderByCreationDateAsc(date);
            final List<UUID> foodIds = entities.stream()
                    .map(MealEntryEntity::getFoodId)
                    .filter(fid -> fid != null)
                    .distinct()
                    .collect(Collectors.toList());
            final Map<UUID, String> foodNameById = foodRepository.findAllById(foodIds).stream()
                    .collect(Collectors.toMap(FoodEntity::getId, FoodEntity::getName));
            return entities.stream()
                    .map(e -> {
                        final String live = foodNameById.get(e.getFoodId());
                        final String display = live != null ? live : e.getFoodName();
                        return mealEntryMapper.toDTO(e, display);
                    })
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());

        final Mono<Optional<TargetsDTO>> snapshotMono = Mono.fromCallable(() ->
                dayTargetSnapshotRepository.findById(date).map(this::toTargetsDTO)
        ).subscribeOn(Schedulers.boundedElastic());

        final Mono<Optional<TargetsDTO>> targetsMono = snapshotMono.flatMap(snapshot -> {
            if (snapshot.isPresent()) {
                return Mono.just(snapshot);
            }
            return nutritionTargetService.getTargets()
                    .map(Optional::of)
                    .onErrorReturn(TargetCalculationException.class, Optional.empty());
        });

        return Mono.zip(entriesMono, targetsMono)
                .map(tuple -> buildDaySummary(date, tuple.getT1(), tuple.getT2().orElse(null)));
    }

    /**
     * Converts a persisted day target snapshot into a {@link TargetsDTO}.
     *
     * @param snapshot the persisted snapshot entity
     * @return the equivalent targets DTO
     */
    private TargetsDTO toTargetsDTO(DayTargetSnapshotEntity snapshot) {
        return new TargetsDTO(
                snapshot.getBmr(),
                snapshot.getMaintenanceKcal(),
                snapshot.getTargetKcal(),
                snapshot.getProteinG(),
                snapshot.getFatG(),
                snapshot.getCarbsG(),
                snapshot.getBasis()
        );
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

    /**
     * Assembles a {@link DaySummaryDTO} from the loaded entries and nullable targets.
     *
     * @param date    the summary date
     * @param entries the mapped entry DTOs
     * @param targets the daily nutrition targets, or null if unavailable
     * @return the assembled day summary
     */
    private DaySummaryDTO buildDaySummary(
            LocalDate date, List<MealEntryDTO> entries, TargetsDTO targets) {
        final MacrosDTO totals = computeTotals(entries);
        final MacrosDTO remaining = targets != null ? computeRemaining(targets, totals) : null;
        return new DaySummaryDTO(date, entries, totals, targets, remaining);
    }

    /**
     * Sums macro-nutrient values across all entries.
     *
     * @param entries the entries to sum
     * @return a MacrosDTO with aggregated totals
     */
    private MacrosDTO computeTotals(List<MealEntryDTO> entries) {
        BigDecimal kcal = BigDecimal.ZERO;
        BigDecimal proteinG = BigDecimal.ZERO;
        BigDecimal carbsG = BigDecimal.ZERO;
        BigDecimal fatG = BigDecimal.ZERO;
        for (final MealEntryDTO entry : entries) {
            kcal = kcal.add(entry.kcal());
            proteinG = proteinG.add(entry.proteinG());
            carbsG = carbsG.add(entry.carbsG());
            fatG = fatG.add(entry.fatG());
        }
        return new MacrosDTO(kcal, proteinG, carbsG, fatG);
    }

    /**
     * Computes the remaining macros as targets minus totals.
     *
     * @param targets the daily nutrition targets
     * @param totals  the already-consumed totals
     * @return a MacrosDTO with the remaining budget
     */
    private MacrosDTO computeRemaining(TargetsDTO targets, MacrosDTO totals) {
        final BigDecimal kcal = BigDecimal.valueOf(targets.targetKcal()).subtract(totals.kcal());
        final BigDecimal proteinG = BigDecimal.valueOf(targets.proteinG()).subtract(totals.proteinG());
        final BigDecimal carbsG = BigDecimal.valueOf(targets.carbsG()).subtract(totals.carbsG());
        final BigDecimal fatG = BigDecimal.valueOf(targets.fatG()).subtract(totals.fatG());
        return new MacrosDTO(kcal, proteinG, carbsG, fatG);
    }
}
