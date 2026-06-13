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
import java.time.LocalDate;
import java.util.ArrayList;
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

    private final MealEntryRepository mealEntryRepository;
    private final FoodRepository foodRepository;
    private final MealEntryMapper mealEntryMapper;
    private final NutritionTargetService nutritionTargetService;
    private final DayTargetSnapshotRepository dayTargetSnapshotRepository;
    private final MealEntryWriteService mealEntryWriteService;

    /**
     * Creates a new MealEntryService with the required dependencies.
     *
     * @param mealEntryRepository         JPA repository for meal entries
     * @param foodRepository              JPA repository for food catalog entries
     * @param mealEntryMapper             MapStruct mapper for entity/DTO conversion
     * @param nutritionTargetService      service for computing daily nutrition targets
     * @param dayTargetSnapshotRepository JPA repository for per-day nutrition target snapshots
     * @param mealEntryWriteService       service owning transactional meal entry write operations
     */
    public MealEntryService(
            MealEntryRepository mealEntryRepository,
            FoodRepository foodRepository,
            MealEntryMapper mealEntryMapper,
            NutritionTargetService nutritionTargetService,
            DayTargetSnapshotRepository dayTargetSnapshotRepository,
            MealEntryWriteService mealEntryWriteService) {
        this.mealEntryRepository = mealEntryRepository;
        this.foodRepository = foodRepository;
        this.mealEntryMapper = mealEntryMapper;
        this.nutritionTargetService = nutritionTargetService;
        this.dayTargetSnapshotRepository = dayTargetSnapshotRepository;
        this.mealEntryWriteService = mealEntryWriteService;
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
        return Mono.fromCallable(() -> mealEntryWriteService.createEntry(date, req))
                .subscribeOn(Schedulers.boundedElastic());
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
        return Mono.fromCallable(() -> mealEntryWriteService.updateEntry(id, req))
                .subscribeOn(Schedulers.boundedElastic());
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
            mealEntryWriteService.deleteEntry(id);
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
            return nutritionTargetService.getTargets(date)
                    .map(Optional::of)
                    .onErrorReturn(TargetCalculationException.class, Optional.empty());
        });

        return Mono.zip(entriesMono, targetsMono)
                .map(tuple -> buildDaySummary(date, tuple.getT1(), tuple.getT2().orElse(null)));
    }

    /**
     * Returns day summaries for every date within the given range (inclusive), in ascending date order.
     * Entries, food names and target snapshots for the whole range are loaded in a single query each,
     * so the cost does not grow with the number of days requested.
     * If the nutrition target service cannot compute live targets (e.g. missing profile/weight),
     * days without a persisted snapshot will have null targets and remaining.
     *
     * @param from the first date to include (inclusive)
     * @param to   the last date to include (inclusive)
     * @return a Mono emitting one day summary per date in the range, ordered ascending by date
     */
    public Mono<List<DaySummaryDTO>> getDays(LocalDate from, LocalDate to) {
        final Mono<Map<LocalDate, List<MealEntryDTO>>> entriesByDateMono = Mono.fromCallable(() -> {
            final List<MealEntryEntity> entities =
                    mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to);
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
                    .collect(Collectors.groupingBy(MealEntryDTO::entryDate));
        }).subscribeOn(Schedulers.boundedElastic());

        final Mono<Map<LocalDate, TargetsDTO>> snapshotsByDateMono = Mono.fromCallable(() ->
                dayTargetSnapshotRepository.findByEntryDateBetween(from, to).stream()
                        .collect(Collectors.toMap(DayTargetSnapshotEntity::getEntryDate, this::toTargetsDTO))
        ).subscribeOn(Schedulers.boundedElastic());

        final Mono<Optional<TargetsDTO>> liveTargetsMono = nutritionTargetService.getTargets()
                .map(Optional::of)
                .onErrorReturn(TargetCalculationException.class, Optional.empty());

        return Mono.zip(entriesByDateMono, snapshotsByDateMono, liveTargetsMono)
                .map(tuple -> {
                    final Map<LocalDate, List<MealEntryDTO>> entriesByDate = tuple.getT1();
                    final Map<LocalDate, TargetsDTO> snapshotsByDate = tuple.getT2();
                    final TargetsDTO liveTargets = tuple.getT3().orElse(null);

                    final List<DaySummaryDTO> summaries = new ArrayList<>();
                    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                        final List<MealEntryDTO> entries = entriesByDate.getOrDefault(date, List.of());
                        final TargetsDTO targets = snapshotsByDate.getOrDefault(date, liveTargets);
                        summaries.add(buildDaySummary(date, entries, targets));
                    }
                    return summaries;
                });
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
