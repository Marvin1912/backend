package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.MealPlanChangelogEntryDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.MealPlanStatDTO;
import com.marvin.nutrition.entity.MealPlanChangelogEntryEntity;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.entity.MealPlanStatEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanChangelogEntryRepository;
import com.marvin.nutrition.repository.MealPlanRepository;
import com.marvin.nutrition.repository.MealPlanSourceRepository;
import com.marvin.nutrition.repository.MealPlanStatRepository;
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
 * repositories and assemblers now that the content is database-backed rather than a cached
 * classpath resource.
 *
 * <p>This test class is a deliberate, pre-approved rewrite (not an incremental edit) of the
 * previous version, which asserted classpath-loading and "same cached instance forever" behavior
 * that no longer applies once reads go through repositories on every call.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanService Tests")
class MealPlanServiceTest {

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private MealPlanStatRepository mealPlanStatRepository;

    @Mock
    private MealPlanChangelogEntryRepository mealPlanChangelogEntryRepository;

    @Mock
    private MealPlanSourceRepository mealPlanSourceRepository;

    @Mock
    private MealPlanSectionAssembler mealPlanSectionAssembler;

    @Mock
    private MealPlanShoppingListAssembler mealPlanShoppingListAssembler;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @InjectMocks
    private MealPlanService mealPlanService;

    private MealPlanEntity mealPlanEntity;
    private List<MealPlanStatEntity> statEntities;
    private List<MealPlanStatDTO> statDTOs;
    private List<MealPlanChangelogEntryEntity> changelogEntities;
    private List<MealPlanChangelogEntryDTO> changelogDTOs;
    private List<MealPlanSourceEntity> sourceEntities;
    private List<MealPlanSourceDTO> sourceDTOs;
    private List<MealPlanSectionDTO> sectionDTOs;
    private List<MealPlanShoppingCategoryDTO> categoryDTOs;

    /** Sets up shared fixtures for each test, mirroring the shape of the real seeded content. */
    @BeforeEach
    void setUp() {
        mealPlanEntity = new MealPlanEntity();
        mealPlanEntity.setId(MealPlanEntity.SINGLETON_ID);
        mealPlanEntity.setEyebrow("Version 3");
        mealPlanEntity.setTitle("Ernährungsplan & Einkaufsliste");
        mealPlanEntity.setDescription("Fettabbau & Muskelerhalt (Cut)");
        mealPlanEntity.setShoppingListTitle("4 · Einkaufsliste für Lidl (1 Woche)");
        mealPlanEntity.setShoppingListNote("4× Kantine Mo–Do · 3× Selbstkochen Fr–So");
        mealPlanEntity.setShoppingListCallout("Whey Protein wird bei Lidl nicht durchgängig geführt.");
        mealPlanEntity.setFooterNote("Nährwerte stammen aus der bestehenden Lebensmitteldatenbank.");

        statEntities = List.of(
                new MealPlanStatEntity(), new MealPlanStatEntity(),
                new MealPlanStatEntity(), new MealPlanStatEntity());
        statDTOs = List.of(
                new MealPlanStatDTO(UUID.randomUUID(), "Tagesbudget (Ø)", "2.416 kcal"),
                new MealPlanStatDTO(UUID.randomUUID(), "Protein", "~184 g"),
                new MealPlanStatDTO(UUID.randomUUID(), "Kohlenhydrate", "~291 g"),
                new MealPlanStatDTO(UUID.randomUUID(), "Fett", "~52 g"));

        changelogEntities = List.of(new MealPlanChangelogEntryEntity(), new MealPlanChangelogEntryEntity());
        changelogDTOs = List.of(
                new MealPlanChangelogEntryDTO(UUID.randomUUID(), "Whey", "80 g/Tag (2×40 g)", "→ 40 g/Tag"),
                new MealPlanChangelogEntryDTO(UUID.randomUUID(), "Magerquark", null, "neu: 300 g/Tag"));

        sourceEntities = List.of(new MealPlanSourceEntity(), new MealPlanSourceEntity());
        sourceDTOs = List.of(
                new MealPlanSourceDTO(UUID.randomUUID(), "Magerquark (fatsecret.de)", "https://www.fatsecret.de/magerquark"),
                new MealPlanSourceDTO(UUID.randomUUID(), "Basmatireis (fatsecret.de)", "https://www.fatsecret.de/basmatireis"));

        final MealPlanRowDTO row =
                new MealPlanRowDTO(UUID.randomUUID(), "Frühstück", "Haferflocken, Whey", "90g/20g", "519", "28,0 g");
        sectionDTOs = List.of(
                new MealPlanSectionDTO(UUID.randomUUID(), "1 · Tagesstruktur", "note", List.of(row), null, "callout"),
                new MealPlanSectionDTO(UUID.randomUUID(), "2 · Wochentage", "note", List.of(row), null, null),
                new MealPlanSectionDTO(UUID.randomUUID(), "3 · Wochenende", "note", List.of(row), null, "callout"));

        final MealPlanShoppingItemDTO plainItem =
                new MealPlanShoppingItemDTO(UUID.randomUUID(), "Rinderhüftsteak", "Lidl", null, null, "435 g");
        final MealPlanShoppingItemDTO badgedItem = new MealPlanShoppingItemDTO(
                UUID.randomUUID(), "Hähnchenbrustfilet", "frisch, Kühltheke", "warn", "nur 1.200 g verfügbar", "1.200 g");
        categoryDTOs = List.of(
                new MealPlanShoppingCategoryDTO(UUID.randomUUID(), "Fleisch & Fisch", List.of(badgedItem, plainItem)),
                new MealPlanShoppingCategoryDTO(UUID.randomUUID(), "Milchprodukte & Eier", List.of(plainItem)));
    }

    /** Stubs every repository/assembler/mapper call needed for a full, successful {@code getMealPlan()} read. */
    private void stubHappyPath() {
        when(mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)).thenReturn(Optional.of(mealPlanEntity));
        when(mealPlanStatRepository.findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID))
                .thenReturn(statEntities);
        when(mealPlanMapper.toStatDTOs(statEntities)).thenReturn(statDTOs);
        when(mealPlanChangelogEntryRepository.findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID))
                .thenReturn(changelogEntities);
        when(mealPlanMapper.toChangelogEntryDTOs(changelogEntities)).thenReturn(changelogDTOs);
        when(mealPlanSourceRepository.findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID))
                .thenReturn(sourceEntities);
        when(mealPlanMapper.toSourceDTOs(sourceEntities)).thenReturn(sourceDTOs);
        when(mealPlanSectionAssembler.assemble(MealPlanEntity.SINGLETON_ID)).thenReturn(sectionDTOs);
        when(mealPlanShoppingListAssembler.assemble(MealPlanEntity.SINGLETON_ID)).thenReturn(categoryDTOs);
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
    @DisplayName("getMealPlan returns the stats mapped from the stat repository")
    void getMealPlan_ReturnsStatsFromRepository() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> {
                    assertEquals(4, dto.stats().size());
                    assertEquals(statDTOs, dto.stats());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getMealPlan returns the changelog entries mapped from the changelog repository")
    void getMealPlan_ReturnsChangelogFromRepository() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> assertEquals(changelogDTOs, dto.changelog()))
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
    @DisplayName("getMealPlan returns a shopping list with title, note, callout and categories with a badged item")
    void getMealPlan_ReturnsShoppingListFromAssembler() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> {
                    assertEquals(mealPlanEntity.getShoppingListTitle(), dto.shoppingList().title());
                    assertEquals(mealPlanEntity.getShoppingListNote(), dto.shoppingList().note());
                    assertEquals(mealPlanEntity.getShoppingListCallout(), dto.shoppingList().callout());
                    assertEquals(2, dto.shoppingList().categories().size());
                    final boolean hasBadge = dto.shoppingList().categories().stream()
                            .flatMap(category -> category.items().stream())
                            .map(MealPlanShoppingItemDTO::badge)
                            .anyMatch(badge -> badge != null);
                    assertTrue(hasBadge);
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
    @DisplayName("getMealPlan reads every table exactly once per call, replacing the former in-memory cache")
    void getMealPlan_CallsEachRepositoryAndAssemblerOnce() {
        stubHappyPath();

        StepVerifier.create(mealPlanService.getMealPlan())
                .assertNext(dto -> assertEquals("Ernährungsplan & Einkaufsliste", dto.title()))
                .verifyComplete();

        verify(mealPlanRepository).findById(MealPlanEntity.SINGLETON_ID);
        verify(mealPlanStatRepository).findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID);
        verify(mealPlanChangelogEntryRepository).findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID);
        verify(mealPlanSourceRepository).findAllByMealPlanIdOrderBySortOrderAsc(MealPlanEntity.SINGLETON_ID);
        verify(mealPlanSectionAssembler).assemble(MealPlanEntity.SINGLETON_ID);
        verify(mealPlanShoppingListAssembler).assemble(MealPlanEntity.SINGLETON_ID);
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
