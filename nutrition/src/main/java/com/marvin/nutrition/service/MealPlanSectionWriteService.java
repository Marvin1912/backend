package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealPlanSectionRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
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

    private final MealPlanSectionRepository mealPlanSectionRepository;
    private final MealPlanRowRepository mealPlanRowRepository;
    private final MealPlanMapper mealPlanMapper;

    /**
     * Creates a new MealPlanSectionWriteService with the required dependencies.
     *
     * @param mealPlanSectionRepository JPA repository for meal-plan sections
     * @param mealPlanRowRepository     JPA repository for meal-plan rows
     * @param mealPlanMapper            MapStruct mapper for entity/DTO conversion
     */
    public MealPlanSectionWriteService(
            MealPlanSectionRepository mealPlanSectionRepository,
            MealPlanRowRepository mealPlanRowRepository,
            MealPlanMapper mealPlanMapper) {
        this.mealPlanSectionRepository = mealPlanSectionRepository;
        this.mealPlanRowRepository = mealPlanRowRepository;
        this.mealPlanMapper = mealPlanMapper;
    }

    /**
     * Updates an existing meal-plan section's title, note and/or callout.
     * Only non-null fields from the request are applied. The returned DTO includes the section's
     * current (unmodified) rows.
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

        final MealPlanSectionEntity saved = mealPlanSectionRepository.save(section);
        final List<MealPlanRowEntity> rowEntities =
                mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(saved.getId());
        final List<MealPlanRowDTO> rows = mealPlanMapper.toRowDTOs(rowEntities);
        return mealPlanMapper.toSectionDTO(saved, rows);
    }

    /**
     * Updates an existing meal-plan row's meal, details, quantity, kcal and/or protein.
     * Only non-null fields from the request are applied.
     * Throws {@link NoSuchElementException} if no row with the given id exists.
     *
     * @param id  the UUID of the row to update
     * @param req the update request
     * @return the updated row DTO
     */
    @Transactional
    public MealPlanRowDTO updateRow(UUID id, UpdateMealPlanRowRequest req) {
        final MealPlanRowEntity row = mealPlanRowRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan row not found: " + id));

        if (req.meal() != null) {
            row.setMeal(req.meal());
        }
        if (req.details() != null) {
            row.setDetails(req.details());
        }
        if (req.qty() != null) {
            row.setQty(req.qty());
        }
        if (req.kcal() != null) {
            row.setKcal(req.kcal());
        }
        if (req.protein() != null) {
            row.setProtein(req.protein());
        }

        final MealPlanRowEntity saved = mealPlanRowRepository.save(row);
        return mealPlanMapper.toRowDTO(saved);
    }
}
