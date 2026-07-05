package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.MealPlanDTO;
import com.marvin.nutrition.dto.MealPlanFooterDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRepository;
import com.marvin.nutrition.repository.MealPlanSourceRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Serves the weekly meal-plan reference document ("Ernährungsplan &amp; Einkaufsliste"), assembled
 * from its normalized database tables. Rows are food-backed and their macros are derived server-side;
 * the plan's content is edited through this module's write endpoints, not through database migrations.
 */
@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanSourceRepository mealPlanSourceRepository;
    private final MealPlanSectionAssembler mealPlanSectionAssembler;
    private final MealPlanMapper mealPlanMapper;

    /**
     * Creates a new MealPlanService with the required dependencies.
     *
     * @param mealPlanRepository       JPA repository for the meal-plan header
     * @param mealPlanSourceRepository JPA repository for footer sources
     * @param mealPlanSectionAssembler assembles sections together with their rows
     * @param mealPlanMapper           MapStruct mapper for entity/DTO conversion
     */
    public MealPlanService(
            MealPlanRepository mealPlanRepository,
            MealPlanSourceRepository mealPlanSourceRepository,
            MealPlanSectionAssembler mealPlanSectionAssembler,
            MealPlanMapper mealPlanMapper) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanSourceRepository = mealPlanSourceRepository;
        this.mealPlanSectionAssembler = mealPlanSectionAssembler;
        this.mealPlanMapper = mealPlanMapper;
    }

    /**
     * Returns the weekly meal-plan reference document, assembled from its database tables.
     * Emits {@link NoSuchElementException} if the singleton meal-plan row is missing.
     *
     * @return a Mono emitting the assembled meal-plan DTO
     */
    public Mono<MealPlanDTO> getMealPlan() {
        return Mono.fromCallable(() -> {
            final MealPlanEntity mealPlan = mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)
                    .orElseThrow(() -> new NoSuchElementException("Meal plan not found"));
            return toMealPlanDTO(mealPlan);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Assembles the full meal-plan DTO from the header entity and its related child tables.
     *
     * @param mealPlan the meal-plan header entity
     * @return the assembled meal-plan DTO
     */
    private MealPlanDTO toMealPlanDTO(MealPlanEntity mealPlan) {
        final Long id = mealPlan.getId();

        final List<MealPlanSourceDTO> sources = mealPlanMapper.toSourceDTOs(
                mealPlanSourceRepository.findAllByMealPlanIdOrderBySortOrderAsc(id));
        final List<MealPlanSectionDTO> sections = mealPlanSectionAssembler.assemble(id);
        final MealPlanFooterDTO footer = new MealPlanFooterDTO(mealPlan.getFooterNote(), sources);

        return new MealPlanDTO(mealPlan.getEyebrow(), mealPlan.getTitle(), mealPlan.getDescription(),
                sections, footer);
    }
}
