package com.marvin.nutrition.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.entity.MealType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Repository integration test covering the meal-plan header, its sources, sections and food-backed
 * rows: ordering, cascade delete, and the {@code countByFoodId} guard used by the food-deletion
 * referential-integrity check (issue #225).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MealPlanRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private MealPlanSourceRepository mealPlanSourceRepository;

    @Autowired
    private MealPlanSectionRepository mealPlanSectionRepository;

    @Autowired
    private MealPlanRowRepository mealPlanRowRepository;

    @Autowired
    private FoodRepository foodRepository;

    private MealPlanEntity mealPlan;
    private FoodEntity food;

    /**
     * Registers dynamic Flyway/datasource properties so migrations run against the Testcontainers database.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "false");
    }

    /**
     * Creates the singleton meal-plan header row and a food catalog entry used as foreign-key
     * targets for this test's children.
     */
    @BeforeEach
    void setUp() {
        final MealPlanEntity newMealPlan = new MealPlanEntity();
        newMealPlan.setId(MealPlanEntity.SINGLETON_ID);
        newMealPlan.setEyebrow("Version 4");
        newMealPlan.setTitle("Ernährungsplan & Einkaufsliste");
        newMealPlan.setDescription("description");
        newMealPlan.setFooterNote("footer note");
        mealPlan = mealPlanRepository.save(newMealPlan);

        final FoodEntity newFood = new FoodEntity();
        newFood.setName("Haferflocken");
        newFood.setKcalPer100(BigDecimal.valueOf(577));
        newFood.setProteinPer100(BigDecimal.valueOf(31));
        newFood.setCarbsPer100(BigDecimal.valueOf(66));
        newFood.setFatPer100(BigDecimal.valueOf(11));
        newFood.setSource(FoodSource.MANUAL);
        food = foodRepository.save(newFood);
    }

    /**
     * Removes the meal-plan row and food entity created for this test, cascading to any remaining children.
     */
    @AfterEach
    void tearDown() {
        mealPlanRepository.deleteById(mealPlan.getId());
        foodRepository.delete(food);
    }

    @Test
    void savesAndFindsSourcesOrderedBySortOrder() {
        saveSource("Basmatireis", "https://example.org/basmatireis", 1);
        saveSource("Magerquark", "https://example.org/magerquark", 0);

        final List<MealPlanSourceEntity> sources =
                mealPlanSourceRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId());

        assertThat(sources).extracting(MealPlanSourceEntity::getLabel)
                .containsExactly("Magerquark", "Basmatireis");
    }

    @Test
    void savesAndFindsSectionsWithFoodBackedRowsOrderedBySortOrder() {
        final MealPlanSectionEntity section = saveSection("2 · Wochentage", "note", 0);

        saveRow(section.getId(), MealType.DINNER, BigDecimal.valueOf(170), 1);
        saveRow(section.getId(), MealType.BREAKFAST, BigDecimal.valueOf(90), 0);

        final List<MealPlanSectionEntity> sections =
                mealPlanSectionRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId());
        assertThat(sections).hasSize(1);

        final List<MealPlanRowEntity> rows =
                mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(section.getId());
        assertThat(rows).extracting(MealPlanRowEntity::getMealType)
                .containsExactly(MealType.BREAKFAST, MealType.DINNER);
        assertThat(rows.getFirst().getFoodId()).isEqualTo(food.getId());
        assertThat(rows.getFirst().getFoodName()).isEqualTo("Haferflocken");
    }

    @Test
    void findFirstByMealPlanSectionIdOrderBySortOrderDescReturnsHighestSortOrderRow() {
        final MealPlanSectionEntity section = saveSection("Section", "note", 0);
        saveRow(section.getId(), MealType.BREAKFAST, BigDecimal.valueOf(90), 0);
        final MealPlanRowEntity highest = saveRow(section.getId(), MealType.DINNER, BigDecimal.valueOf(170), 2);
        saveRow(section.getId(), MealType.SNACK, BigDecimal.valueOf(50), 1);

        final Optional<MealPlanRowEntity> result =
                mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(section.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(highest.getId());
        assertThat(result.get().getSortOrder()).isEqualTo(2);
    }

    @Test
    void findFirstByMealPlanSectionIdOrderBySortOrderDescReturnsEmptyForSectionWithNoRows() {
        final MealPlanSectionEntity section = saveSection("Empty section", "note", 0);

        final Optional<MealPlanRowEntity> result =
                mealPlanRowRepository.findFirstByMealPlanSectionIdOrderBySortOrderDesc(section.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void newRowAfterDeletingMiddleRowDoesNotCollideWithRemainingSortOrders() {
        final MealPlanSectionEntity section = saveSection("Section", "note", 0);
        final MealPlanRowEntity first = saveRow(section.getId(), MealType.BREAKFAST, BigDecimal.valueOf(90), 0);
        final MealPlanRowEntity middle = saveRow(section.getId(), MealType.LUNCH, BigDecimal.valueOf(150), 1);
        final MealPlanRowEntity last = saveRow(section.getId(), MealType.DINNER, BigDecimal.valueOf(170), 2);

        mealPlanRowRepository.delete(middle);
        mealPlanRowRepository.flush();

        // Mirrors MealPlanSectionWriteService's next-sort-order derivation: max(existing) + 1, not count().
        final int nextSortOrder = mealPlanRowRepository
                .findFirstByMealPlanSectionIdOrderBySortOrderDesc(section.getId())
                .map(row -> row.getSortOrder() + 1)
                .orElse(0);
        saveRow(section.getId(), MealType.SNACK, BigDecimal.valueOf(50), nextSortOrder);

        final List<MealPlanRowEntity> remainingRows =
                mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(section.getId());

        assertThat(remainingRows).extracting(MealPlanRowEntity::getSortOrder)
                .doesNotHaveDuplicates()
                .containsExactly(0, 2, 3);
        assertThat(remainingRows).extracting(MealPlanRowEntity::getId)
                .contains(first.getId(), last.getId());
    }

    @Test
    void newSectionDefaultsDayCountToOne() {
        final MealPlanSectionEntity section = saveSection("Section", "note", 0);

        assertThat(section.getDayCount()).isEqualTo(1);
    }

    @Test
    void savesAndFindsSectionWithExplicitDayCount() {
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setMealPlanId(mealPlan.getId());
        section.setTitle("2 · Wochentage");
        section.setNote("note");
        section.setSortOrder(0);
        section.setDayCount(4);
        final MealPlanSectionEntity saved = mealPlanSectionRepository.save(section);

        final MealPlanSectionEntity found = mealPlanSectionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getDayCount()).isEqualTo(4);
    }

    @Test
    void countByFoodIdCountsRowsReferencingTheFood() {
        final MealPlanSectionEntity section = saveSection("Section", "note", 0);
        saveRow(section.getId(), MealType.BREAKFAST, BigDecimal.valueOf(90), 0);
        saveRow(section.getId(), MealType.SNACK, BigDecimal.valueOf(50), 1);

        assertThat(mealPlanRowRepository.countByFoodId(food.getId())).isEqualTo(2);
        assertThat(mealPlanRowRepository.countByFoodId(UUID.randomUUID())).isEqualTo(0);
    }

    @Test
    void deletingMealPlanCascadesToSourcesSectionsAndRows() {
        saveSource("Magerquark", "https://example.org/magerquark", 0);
        final MealPlanSectionEntity section = saveSection("Section", "note", 0);
        saveRow(section.getId(), MealType.BREAKFAST, BigDecimal.valueOf(90), 0);

        mealPlanRepository.deleteById(mealPlan.getId());
        mealPlanRepository.flush();

        assertThat(mealPlanSourceRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId())).isEmpty();
        assertThat(mealPlanSectionRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId())).isEmpty();
        assertThat(mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(section.getId())).isEmpty();
    }

    private MealPlanSourceEntity saveSource(String label, String url, int sortOrder) {
        final MealPlanSourceEntity source = new MealPlanSourceEntity();
        source.setMealPlanId(mealPlan.getId());
        source.setLabel(label);
        source.setUrl(url);
        source.setSortOrder(sortOrder);
        return mealPlanSourceRepository.save(source);
    }

    private MealPlanSectionEntity saveSection(String title, String note, int sortOrder) {
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setMealPlanId(mealPlan.getId());
        section.setTitle(title);
        section.setNote(note);
        section.setSortOrder(sortOrder);
        return mealPlanSectionRepository.save(section);
    }

    private MealPlanRowEntity saveRow(UUID sectionId, MealType mealType, BigDecimal quantityG, int sortOrder) {
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setMealPlanSectionId(sectionId);
        row.setMealType(mealType);
        row.setFoodId(food.getId());
        row.setFoodName(food.getName());
        row.setQuantityG(quantityG);
        row.setKcal(BigDecimal.valueOf(519));
        row.setProteinG(BigDecimal.valueOf(28));
        row.setCarbsG(BigDecimal.valueOf(60));
        row.setFatG(BigDecimal.valueOf(10));
        row.setSortOrder(sortOrder);
        return mealPlanRowRepository.save(row);
    }
}
