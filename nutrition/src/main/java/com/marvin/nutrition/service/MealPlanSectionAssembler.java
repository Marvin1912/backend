package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRowRepository;
import com.marvin.nutrition.repository.MealPlanSectionRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Assembles the meal plan's ordered list of sections together with each section's ordered rows. */
@Component
public class MealPlanSectionAssembler {

    private final MealPlanSectionRepository mealPlanSectionRepository;
    private final MealPlanRowRepository mealPlanRowRepository;
    private final MealPlanMapper mealPlanMapper;

    /**
     * Creates a new MealPlanSectionAssembler.
     *
     * @param mealPlanSectionRepository JPA repository for meal plan sections
     * @param mealPlanRowRepository    JPA repository for meal plan rows
     * @param mealPlanMapper           mapper for converting entities into DTOs
     */
    public MealPlanSectionAssembler(
            MealPlanSectionRepository mealPlanSectionRepository,
            MealPlanRowRepository mealPlanRowRepository,
            MealPlanMapper mealPlanMapper) {
        this.mealPlanSectionRepository = mealPlanSectionRepository;
        this.mealPlanRowRepository = mealPlanRowRepository;
        this.mealPlanMapper = mealPlanMapper;
    }

    /**
     * Loads and assembles all sections of the given meal plan, each with its ordered rows.
     *
     * @param mealPlanId the id of the meal plan
     * @return the ordered list of section DTOs
     */
    public List<MealPlanSectionDTO> assemble(Long mealPlanId) {
        final List<MealPlanSectionEntity> sections =
                mealPlanSectionRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlanId);
        return sections.stream()
                .map(this::toSectionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Loads a section's rows and maps the section together with them to a DTO.
     *
     * @param section the section entity
     * @return the assembled section DTO
     */
    private MealPlanSectionDTO toSectionDTO(MealPlanSectionEntity section) {
        final List<MealPlanRowEntity> rowEntities =
                mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(section.getId());
        final List<MealPlanRowDTO> rows = mealPlanMapper.toRowDTOs(rowEntities);
        return mealPlanMapper.toSectionDTO(section, rows);
    }
}
