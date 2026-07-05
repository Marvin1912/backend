package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateMealPlanRowRequest;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRepository;
import com.marvin.nutrition.repository.MealPlanSourceRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional write operations for the weekly meal-plan reference document, so that
 * content corrections can be made through the REST API instead of hand-written SQL or new Flyway
 * migrations.
 *
 * <p>Header and footer-source updates are handled directly by this service. Section/row updates
 * are delegated to {@link MealPlanSectionWriteService}, to keep this facade's constructor within
 * the project's parameter-count limit while still exposing a single write-service entry point per
 * the module's read-side {@code MealPlanService} counterpart.</p>
 *
 * <p>All methods in this service perform blocking repository access (directly or via the delegate
 * service) and must only be called from a thread already running on a blocking-friendly scheduler
 * (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}), and must be invoked from
 * outside this bean so that Spring's transactional proxy is applied.</p>
 */
@Service
public class MealPlanWriteService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanSourceRepository mealPlanSourceRepository;
    private final MealPlanMapper mealPlanMapper;
    private final MealPlanSectionWriteService mealPlanSectionWriteService;

    /**
     * Creates a new MealPlanWriteService with the required dependencies.
     *
     * @param mealPlanRepository          JPA repository for the meal-plan header
     * @param mealPlanSourceRepository    JPA repository for footer sources
     * @param mealPlanMapper              MapStruct mapper for entity/DTO conversion
     * @param mealPlanSectionWriteService delegate owning section/row write operations
     */
    public MealPlanWriteService(
            MealPlanRepository mealPlanRepository,
            MealPlanSourceRepository mealPlanSourceRepository,
            MealPlanMapper mealPlanMapper,
            MealPlanSectionWriteService mealPlanSectionWriteService) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanSourceRepository = mealPlanSourceRepository;
        this.mealPlanMapper = mealPlanMapper;
        this.mealPlanSectionWriteService = mealPlanSectionWriteService;
    }

    /**
     * Updates the meal plan's header content (eyebrow, title, description, footer note).
     * Only non-null fields from the request are applied.
     * Throws {@link NoSuchElementException} if the singleton meal-plan row is missing (should not happen
     * once the seeding migration has run).
     *
     * @param req the update request
     * @return the updated header DTO
     */
    @Transactional
    public MealPlanHeaderDTO updateMealPlan(UpdateMealPlanRequest req) {
        final MealPlanEntity entity = mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)
                .orElseThrow(() -> new NoSuchElementException("Meal plan not found"));

        if (req.eyebrow() != null) {
            entity.setEyebrow(req.eyebrow());
        }
        if (req.title() != null) {
            entity.setTitle(req.title());
        }
        if (req.description() != null) {
            entity.setDescription(req.description());
        }
        if (req.footerNote() != null) {
            entity.setFooterNote(req.footerNote());
        }

        final MealPlanEntity saved = mealPlanRepository.save(entity);
        return new MealPlanHeaderDTO(saved.getEyebrow(), saved.getTitle(), saved.getDescription(), saved.getFooterNote());
    }

    /**
     * Updates an existing footer source's label and/or URL.
     * Only non-null fields from the request are applied.
     * Throws {@link NoSuchElementException} if no source with the given id exists.
     *
     * @param id  the UUID of the source to update
     * @param req the update request
     * @return the updated source DTO
     */
    @Transactional
    public MealPlanSourceDTO updateSource(UUID id, UpdateMealPlanSourceRequest req) {
        final MealPlanSourceEntity source = mealPlanSourceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan source not found: " + id));

        if (req.label() != null) {
            source.setLabel(req.label());
        }
        if (req.url() != null) {
            source.setUrl(req.url());
        }

        final MealPlanSourceEntity saved = mealPlanSourceRepository.save(source);
        return mealPlanMapper.toSourceDTO(saved);
    }

    /**
     * Deletes the footer source with the given id.
     * Throws {@link NoSuchElementException} if no source with the given id exists.
     *
     * @param id the UUID of the source to delete
     */
    @Transactional
    public void deleteSource(UUID id) {
        final MealPlanSourceEntity source = mealPlanSourceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan source not found: " + id));
        mealPlanSourceRepository.delete(source);
    }

    /**
     * Updates an existing meal-plan section's title, note and/or callout.
     * Delegates to {@link MealPlanSectionWriteService#updateSection(UUID, UpdateMealPlanSectionRequest)}.
     *
     * @param id  the UUID of the section to update
     * @param req the update request
     * @return the updated section DTO, including its rows
     */
    public MealPlanSectionDTO updateSection(UUID id, UpdateMealPlanSectionRequest req) {
        return mealPlanSectionWriteService.updateSection(id, req);
    }

    /**
     * Creates a new food-backed row within the given section.
     * Delegates to {@link MealPlanSectionWriteService#addRow(UUID, CreateMealPlanRowRequest)}.
     *
     * @param sectionId the UUID of the section to add the row to
     * @param req       the create request
     * @return the created row DTO
     */
    public MealPlanRowDTO addRow(UUID sectionId, CreateMealPlanRowRequest req) {
        return mealPlanSectionWriteService.addRow(sectionId, req);
    }

    /**
     * Creates multiple food-backed rows within the given section in a single transaction.
     * Delegates to {@link MealPlanSectionWriteService#addRows(UUID, List)}.
     *
     * @param sectionId the UUID of the section to add the rows to
     * @param requests  the create requests, one per row to be created
     * @return the created row DTOs, in the same order as {@code requests}
     */
    public List<MealPlanRowDTO> addRows(UUID sectionId, List<CreateMealPlanRowRequest> requests) {
        return mealPlanSectionWriteService.addRows(sectionId, requests);
    }

    /**
     * Updates an existing meal-plan row's meal type, referenced food and/or quantity.
     * Delegates to {@link MealPlanSectionWriteService#updateRow(UUID, UpdateMealPlanRowRequest)}.
     *
     * @param id  the UUID of the row to update
     * @param req the update request
     * @return the updated row DTO
     */
    public MealPlanRowDTO updateRow(UUID id, UpdateMealPlanRowRequest req) {
        return mealPlanSectionWriteService.updateRow(id, req);
    }

    /**
     * Deletes the meal-plan row with the given id.
     * Delegates to {@link MealPlanSectionWriteService#deleteRow(UUID)}.
     *
     * @param id the UUID of the row to delete
     */
    public void deleteRow(UUID id) {
        mealPlanSectionWriteService.deleteRow(id);
    }
}
