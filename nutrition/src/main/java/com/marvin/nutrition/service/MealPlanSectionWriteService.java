package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateMealPlanRowRequest;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealPlanSectionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional write operations for meal-plan sections and rows.
 *
 * <p>All methods in this service perform blocking repository access and must only be called from
 * a thread already running on a blocking-friendly scheduler (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}),
 * and must be invoked from outside this bean so that Spring's transactional proxy is applied.</p>
 */
@Service
public class MealPlanSectionWriteService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MealPlanSectionRepository mealPlanSectionRepository;
    private final MealPlanRowRepository mealPlanRowRepository;
    private final FoodRepository foodRepository;
    private final MealPlanMapper mealPlanMapper;

    /**
     * Creates a new MealPlanSectionWriteService with the required dependencies.
     *
     * @param mealPlanSectionRepository JPA repository for meal-plan sections
     * @param mealPlanRowRepository     JPA repository for meal-plan rows
     * @param foodRepository            JPA repository for food catalog entries
     * @param mealPlanMapper            MapStruct mapper for entity/DTO conversion
     */
    public MealPlanSectionWriteService(
            MealPlanSectionRepository mealPlanSectionRepository,
            MealPlanRowRepository mealPlanRowRepository,
            FoodRepository foodRepository,
            MealPlanMapper mealPlanMapper) {
        this.mealPlanSectionRepository = mealPlanSectionRepository;
        this.mealPlanRowRepository = mealPlanRowRepository;
        this.foodRepository = foodRepository;
        this.mealPlanMapper = mealPlanMapper;
    }

    /**
     * Updates an existing meal-plan section's title, note, callout and/or day count.
     * Only non-null fields from the request are applied. The returned DTO
     * includes the section's current (unmodified) rows.
     * Throws {@link NoSuchElementException} if no section with the given id exists.
     *
     * @param id  the UUID of the section to update
     * @param req the update request
     * @return the updated section DTO, including its rows
     */
    @Transactional
    public MealPlanSectionDTO updateSection(UUID id, UpdateMealPlanSectionRequest req) {
        final MealPlanSectionEntity section = mealPlanSectionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan section not found: " + id));

        if (req.title() != null) {
            section.setTitle(req.title());
        }
        if (req.note() != null) {
            section.setNote(req.note());
        }
        if (req.callout() != null) {
            section.setCallout(req.callout());
        }
        if (req.dayCount() != null) {
            section.setDayCount(req.dayCount());
        }

        final MealPlanSectionEntity saved = mealPlanSectionRepository.save(section);
        return toSectionDTOWithRows(saved);
    }

    /**
     * Creates a new food-backed row within the given section. Macros are derived server-side from
     * the referenced food's per-100g values and the supplied quantity. The row's sort order is
     * derived from the highest existing sort order in the section (not a count), so that a row
     * deleted from the middle of the section never causes a new row to collide with a sort order
     * still in use by a remaining row.
     * Throws {@link NoSuchElementException} if the section or the referenced food is not found.
     *
     * @param sectionId the UUID of the section to add the row to
     * @param req       the create request
     * @return the created row DTO
     */
    @Transactional
    public MealPlanRowDTO addRow(UUID sectionId, CreateMealPlanRowRequest req) {
        final MealPlanSectionEntity section = mealPlanSectionRepository.findById(sectionId)
                .orElseThrow(() -> new NoSuchElementException("Meal plan section not found: " + sectionId));
        final FoodEntity food = foodRepository.findById(req.foodId())
                .orElseThrow(() -> new NoSuchElementException("Food not found: " + req.foodId()));

        final MealPlanRowEntity saved = buildAndSaveRow(section.getId(), req, food, nextSortOrder(sectionId));
        return mealPlanMapper.toRowDTO(saved);
    }

    /**
     * Creates multiple food-backed rows within the given section in a single transaction.
     * All referenced foods are loaded in a single batch query rather than one lookup per row.
     * If any referenced food is not found, the whole batch is rolled back.
     * Sort orders are assigned sequentially starting from the section's highest existing sort
     * order + 1 (see {@link #addRow(UUID, CreateMealPlanRowRequest)} for why this matters).
     * Throws {@link NoSuchElementException} if the section or any referenced food is not found.
     *
     * @param sectionId the UUID of the section to add the rows to
     * @param requests  the create requests, one per row to be created
     * @return the created row DTOs, in the same order as {@code requests}
     */
    @Transactional
    public List<MealPlanRowDTO> addRows(UUID sectionId, List<CreateMealPlanRowRequest> requests) {
        final MealPlanSectionEntity section = mealPlanSectionRepository.findById(sectionId)
                .orElseThrow(() -> new NoSuchElementException("Meal plan section not found: " + sectionId));
        final Map<UUID, FoodEntity> foodsById = loadFoodsByIds(requests);

        int nextSortOrder = nextSortOrder(sectionId);
        final List<MealPlanRowDTO> created = new ArrayList<>();
        for (final CreateMealPlanRowRequest req : requests) {
            final FoodEntity food = foodsById.get(req.foodId());
            if (food == null) {
                throw new NoSuchElementException("Food not found: " + req.foodId());
            }
            final MealPlanRowEntity saved = buildAndSaveRow(section.getId(), req, food, nextSortOrder);
            created.add(mealPlanMapper.toRowDTO(saved));
            nextSortOrder++;
        }
        return created;
    }

    /**
     * Updates an existing meal-plan row's meal type, referenced food and/or quantity. {@code foodId}
     * and {@code quantityG} are always required and macros are re-snapshotted from the referenced
     * food's per-100g values on every update; {@code mealType} is only applied when non-null.
     * Throws {@link NoSuchElementException} if no row or no food with the given ids exists.
     *
     * @param id  the UUID of the row to update
     * @param req the update request
     * @return the updated row DTO
     */
    @Transactional
    public MealPlanRowDTO updateRow(UUID id, UpdateMealPlanRowRequest req) {
        final MealPlanRowEntity row = mealPlanRowRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan row not found: " + id));
        final FoodEntity food = foodRepository.findById(req.foodId())
                .orElseThrow(() -> new NoSuchElementException("Food not found: " + req.foodId()));

        if (req.mealType() != null) {
            row.setMealType(req.mealType());
        }
        applyFoodSnapshot(row, food, req.quantityG());

        final MealPlanRowEntity saved = mealPlanRowRepository.save(row);
        return mealPlanMapper.toRowDTO(saved);
    }

    /**
     * Deletes the meal-plan row with the given id.
     * Throws {@link NoSuchElementException} if no row with that id exists.
     *
     * @param id the UUID of the row to delete
     */
    @Transactional
    public void deleteRow(UUID id) {
        final MealPlanRowEntity row = mealPlanRowRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan row not found: " + id));
        mealPlanRowRepository.delete(row);
    }

    /**
     * Loads all foods referenced by the given create requests in a single query, keyed by id.
     *
     * @param requests the create requests whose referenced foods should be loaded
     * @return a map of food id to food entity, containing only the foods that were found
     */
    private Map<UUID, FoodEntity> loadFoodsByIds(List<CreateMealPlanRowRequest> requests) {
        final List<UUID> foodIds = requests.stream()
                .map(CreateMealPlanRowRequest::foodId)
                .distinct()
                .collect(Collectors.toList());
        return foodRepository.findAllById(foodIds).stream()
                .collect(Collectors.toMap(FoodEntity::getId, food -> food));
    }

    /**
     * Builds a new row entity for the given section, snapshotting the food's macros for the given
     * quantity, and saves it.
     *
     * @param sectionId the id of the owning section
     * @param req       the create request
     * @param food      the already-resolved food entity referenced by the request
     * @param sortOrder the display position to assign to the new row
     * @return the saved row entity
     */
    private MealPlanRowEntity buildAndSaveRow(UUID sectionId, CreateMealPlanRowRequest req, FoodEntity food, int sortOrder) {
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setMealPlanSectionId(sectionId);
        row.setMealType(req.mealType());
        applyFoodSnapshot(row, food, req.quantityG());
        row.setSortOrder(sortOrder);

        return mealPlanRowRepository.save(row);
    }

    /**
     * Applies the food reference and its macro snapshot (for the given quantity) onto the row.
     * Shared by row creation and row updates so the snapshot logic exists in exactly one place.
     *
     * @param row       the row entity to populate
     * @param food      the referenced food entity
     * @param quantityG the portion size in grams
     */
    private void applyFoodSnapshot(MealPlanRowEntity row, FoodEntity food, BigDecimal quantityG) {
        row.setFoodId(food.getId());
        row.setFoodName(food.getName());
        row.setQuantityG(quantityG);
        row.setKcal(snapshot(food.getKcalPer100(), quantityG));
        row.setProteinG(snapshot(food.getProteinPer100(), quantityG));
        row.setCarbsG(snapshot(food.getCarbsPer100(), quantityG));
        row.setFatG(snapshot(food.getFatPer100(), quantityG));
    }

    /**
     * Computes the next sort order for a new row in the given section, as the section's highest
     * existing sort order + 1 (or 0 if the section has no rows yet). Deriving this from the max
     * rather than a count means a row deleted from the middle of the section can never cause a new
     * row's sort order to collide with a sort order still in use by a remaining row.
     *
     * @param sectionId the id of the section
     * @return the next sort order to assign
     */
    private int nextSortOrder(UUID sectionId) {
        return mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(sectionId)
                .map(row -> row.getSortOrder() + 1)
                .orElse(0);
    }

    /**
     * Loads a section's rows and maps the section together with them to a DTO.
     *
     * @param section the section entity
     * @return the assembled section DTO
     */
    private MealPlanSectionDTO toSectionDTOWithRows(MealPlanSectionEntity section) {
        final List<MealPlanRowEntity> rowEntities =
                mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(section.getId());
        final List<MealPlanRowDTO> rows = mealPlanMapper.toRowDTOs(rowEntities);
        return mealPlanMapper.toSectionDTO(section, rows);
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
