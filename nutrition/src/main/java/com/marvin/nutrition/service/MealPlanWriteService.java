package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.CreateMealPlanChangelogEntryRequest;
import com.marvin.nutrition.dto.MealPlanChangelogEntryDTO;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.MealPlanStatDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingCategoryRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingItemRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
import com.marvin.nutrition.dto.UpdateMealPlanStatRequest;
import com.marvin.nutrition.entity.MealPlanChangelogEntryEntity;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.entity.MealPlanStatEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanChangelogEntryRepository;
import com.marvin.nutrition.repository.MealPlanRepository;
import com.marvin.nutrition.repository.MealPlanSourceRepository;
import com.marvin.nutrition.repository.MealPlanStatRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional write operations for the weekly meal-plan reference document, so that
 * content corrections can be made through the REST API instead of hand-written SQL or new Flyway
 * migrations.
 *
 * <p>Header, headline-stat, footer-source and changelog updates are handled directly by this
 * service. Section/row and shopping-category/item updates are delegated to
 * {@link MealPlanSectionWriteService} and {@link MealPlanShoppingListWriteService} respectively, to
 * keep this facade's constructor within the project's parameter-count limit while still exposing a
 * single write-service entry point per the module's read-side {@code MealPlanService} counterpart.</p>
 *
 * <p>All methods in this service perform blocking repository access (directly or via the delegate
 * services) and must only be called from a thread already running on a blocking-friendly scheduler
 * (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}), and must be invoked from
 * outside this bean so that Spring's transactional proxy is applied.</p>
 */
@Service
public class MealPlanWriteService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanStatRepository mealPlanStatRepository;
    private final MealPlanSourceRepository mealPlanSourceRepository;
    private final MealPlanChangelogEntryRepository mealPlanChangelogEntryRepository;
    private final MealPlanMapper mealPlanMapper;
    private final MealPlanSectionWriteService mealPlanSectionWriteService;
    private final MealPlanShoppingListWriteService mealPlanShoppingListWriteService;

    /**
     * Creates a new MealPlanWriteService with the required dependencies.
     *
     * @param mealPlanRepository                JPA repository for the meal-plan header
     * @param mealPlanStatRepository            JPA repository for headline stats
     * @param mealPlanSourceRepository          JPA repository for footer sources
     * @param mealPlanChangelogEntryRepository  JPA repository for changelog entries
     * @param mealPlanMapper                    MapStruct mapper for entity/DTO conversion
     * @param mealPlanSectionWriteService       delegate owning section/row write operations
     * @param mealPlanShoppingListWriteService  delegate owning shopping category/item write operations
     */
    public MealPlanWriteService(
            MealPlanRepository mealPlanRepository,
            MealPlanStatRepository mealPlanStatRepository,
            MealPlanSourceRepository mealPlanSourceRepository,
            MealPlanChangelogEntryRepository mealPlanChangelogEntryRepository,
            MealPlanMapper mealPlanMapper,
            MealPlanSectionWriteService mealPlanSectionWriteService,
            MealPlanShoppingListWriteService mealPlanShoppingListWriteService) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanStatRepository = mealPlanStatRepository;
        this.mealPlanSourceRepository = mealPlanSourceRepository;
        this.mealPlanChangelogEntryRepository = mealPlanChangelogEntryRepository;
        this.mealPlanMapper = mealPlanMapper;
        this.mealPlanSectionWriteService = mealPlanSectionWriteService;
        this.mealPlanShoppingListWriteService = mealPlanShoppingListWriteService;
    }

    /**
     * Updates the meal plan's header content (eyebrow, title, description, shopping-list title/note/callout,
     * footer note). Only non-null fields from the request are applied.
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
        if (req.shoppingListTitle() != null) {
            entity.setShoppingListTitle(req.shoppingListTitle());
        }
        if (req.shoppingListNote() != null) {
            entity.setShoppingListNote(req.shoppingListNote());
        }
        if (req.shoppingListCallout() != null) {
            entity.setShoppingListCallout(req.shoppingListCallout());
        }
        if (req.footerNote() != null) {
            entity.setFooterNote(req.footerNote());
        }

        final MealPlanEntity saved = mealPlanRepository.save(entity);
        return new MealPlanHeaderDTO(
                saved.getEyebrow(), saved.getTitle(), saved.getDescription(),
                saved.getShoppingListTitle(), saved.getShoppingListNote(), saved.getShoppingListCallout(),
                saved.getFooterNote());
    }

    /**
     * Updates an existing headline statistic's label and/or value.
     * Only non-null fields from the request are applied.
     * Throws {@link NoSuchElementException} if no stat with the given id exists.
     *
     * @param id  the UUID of the stat to update
     * @param req the update request
     * @return the updated stat DTO
     */
    @Transactional
    public MealPlanStatDTO updateStat(UUID id, UpdateMealPlanStatRequest req) {
        final MealPlanStatEntity stat = mealPlanStatRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meal plan stat not found: " + id));

        if (req.label() != null) {
            stat.setLabel(req.label());
        }
        if (req.value() != null) {
            stat.setValue(req.value());
        }

        final MealPlanStatEntity saved = mealPlanStatRepository.save(stat);
        return mealPlanMapper.toStatDTO(saved);
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
     * Appends a new entry to the meal plan's changelog. The changelog is an append-only historical
     * log; there is no update or delete operation for existing entries.
     *
     * @param req the create request, containing the required tag/text/sortOrder and optional was
     * @return the created changelog entry DTO
     */
    @Transactional
    public MealPlanChangelogEntryDTO addChangelogEntry(CreateMealPlanChangelogEntryRequest req) {
        final MealPlanChangelogEntryEntity entity = new MealPlanChangelogEntryEntity();
        entity.setMealPlanId(MealPlanEntity.SINGLETON_ID);
        entity.setTag(req.tag());
        entity.setWas(req.was());
        entity.setText(req.text());
        entity.setSortOrder(req.sortOrder());

        final MealPlanChangelogEntryEntity saved = mealPlanChangelogEntryRepository.save(entity);
        return mealPlanMapper.toChangelogEntryDTO(saved);
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
     * Updates an existing meal-plan row's meal, details, quantity, kcal and/or protein.
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
     * Updates an existing shopping category's title.
     * Delegates to
     * {@link MealPlanShoppingListWriteService#updateShoppingCategory(UUID, UpdateMealPlanShoppingCategoryRequest)}.
     *
     * @param id  the UUID of the category to update
     * @param req the update request
     * @return the updated category DTO, including its items
     */
    public MealPlanShoppingCategoryDTO updateShoppingCategory(UUID id, UpdateMealPlanShoppingCategoryRequest req) {
        return mealPlanShoppingListWriteService.updateShoppingCategory(id, req);
    }

    /**
     * Updates an existing shopping item's name, brand, badge, badge text and/or quantity.
     * Delegates to
     * {@link MealPlanShoppingListWriteService#updateShoppingItem(UUID, UpdateMealPlanShoppingItemRequest)}.
     *
     * @param id  the UUID of the item to update
     * @param req the update request
     * @return the updated item DTO
     */
    public MealPlanShoppingItemDTO updateShoppingItem(UUID id, UpdateMealPlanShoppingItemRequest req) {
        return mealPlanShoppingListWriteService.updateShoppingItem(id, req);
    }
}
