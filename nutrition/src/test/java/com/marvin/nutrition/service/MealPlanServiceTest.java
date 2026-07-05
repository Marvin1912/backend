package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanRepository;
import com.marvin.nutrition.repository.MealPlanSourceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link MealPlanService} covering assembly of the meal-plan DTO from its
 * repositories and assemblers now that rows are food-backed and stats/changelog/shopping-list have
 * been removed entirely (issue #225 rewrite).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanService Tests")
class MealPlanServiceTest {

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private MealPlanSourceRepository mealPlanSourceRepository;

    @Mock
    private MealPlanSectionAssembler mealPlanSectionAssembler;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @InjectMocks
    private MealPlanService mealPlanService;

    private MealPlanEntity mealPlanEntity;
    private List<MealPlanSourceEntity> sourceEntities;
    private List<MealPlanSourceDTO> sourceDTOs;
    private List<MealPlanSectionDTO> sectionDTOs;

    /** Sets up shared fixtures for each test, mirroring the shape of the real seeded content. */
    @BeforeEach
    void setUp() {
        mealPlanEntity = new MealPlanEntity();
        mealPlanEntity.setId(MealPlanEntity.SINGLETON_ID);
        mealPlanEntity.setEyebrow("Version 4");
        mealPlanEntity.setTitle("Ernährungsplan & Einkaufsliste");
        mealPlanEntity.setDescription("Fettabbau & Muskelerhalt (Cut)");
        mealPlanEntity.setFooterNote("Nährwerte stammen aus der bestehenden Lebensmitteldatenbank.");

        sourceEntities = List.of(new MealPlanSourceEntity(), new MealPlanSourceEntity());
        sourceDTOs = List.of(
                new MealPlanSourceDTO(UUID.randomUUID(), "Magerquark (fatsecret.de)", "https://www.fatsecret.de/magerquark"),
                new MealPlanSourceDTO(UUID.randomUUID(), "Basmatireis (fatsecret.de)", "https://www.fatsecret.de/basmatireis"));

        final MealPlanRowDTO row = new MealPlanRowDTO(
                UUID.randomUUID(), MealType.BREAKFAST, UUID.randomUUID(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));
        sectionDTOs = List.of(
                new MealPlanSectionDTO(UUID.randomUUID(), "1 · Tagesstruktur", "note", List.of(row), "callout"),
                new MealPlanSectionDTO(UUID.randomUUID(), "2 · Wochentage", "note", List.of(row), null),
                new MealPlanSectionDTO(UUID.randomUUID(), "3 · Wochenende", "note", List.of(row), "callout"));
    }

    /** Stubs every repository/assembler/mapper call needed for a full, successful {@code getMealPlan()} read. */
    private void stubHappyPath() {
        when(mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)).thenReturn(Optional.of(mealPlanEntity));
        when(mealPlanSourceRepository.findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID))
                .thenReturn(sourceEntities);
        when(mealPlanMapper.toSourceDTOs(sourceEntities)).thenReturn(sourceDTOs);
        when(mealPlanSectionAssembler.assemble(MealPlanEntity.SINGLETON_ID)).thenReturn(sectionDTOs);
    }

    // -----------------------------------------------------------------------
    // getMealPlan
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getMealPlan returns the meal plan with the header's title")
    void getMealPlan_ReturnsMealPlanWithCorrectTitle() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> assertEquals("Ernährungsplan & Einkaufsliste", dto.title()))
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns the sections assembled by the section assembler")
    void getMealPlan_ReturnsSectionsFromAssembler() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> {
                    assertEquals(3, dto.sections().size());
                    assertEquals(sectionDTOs, dto.sections());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns a footer with the closing note and mapped sources")
    void getMealPlan_ReturnsFooterFromRepository() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> {
                    assertEquals(mealPlanEntity.getFooterNote(), dto.footer().note());
                    assertEquals(sourceDTOs, dto.footer().sources());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan reads every table exactly once per call")
    void getMealPlan_CallsEachRepositoryAndAssemblerOnce() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> assertEquals("Ernährungsplan & Einkaufsliste", dto.title()))
                .verifyComplete();

        verify(mealPlanRepository).findById(MealPlanEntity.SINGLETON_ID);
        verify(mealPlanSourceRepository).findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID);
        verify(mealPlanSectionAssembler).assemble(MealPlanEntity.SINGLETON_ID);
    }

    @Test
    @DisplayName("getMealPlan emits NoSuchElementException when the singleton meal-plan row is missing")
    void getMealPlan_MealPlanNotFound_EmitsNoSuchElementException() {
        when(mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        StepVerifier.create(mealPlanService.getMealPlan())
                .expectError(NoSuchElementException.class)
                .verify();
    }
}
