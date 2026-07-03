package com.marvin.nutrition.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.nutrition.entity.MealPlanChangelogEntryEntity;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.entity.MealPlanRowEntity;
import com.marvin.nutrition.entity.MealPlanSectionEntity;
import com.marvin.nutrition.entity.MealPlanShoppingCategoryEntity;
import com.marvin.nutrition.entity.MealPlanShoppingItemEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.entity.MealPlanStatEntity;
import java.util.List;
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

/** Repository integration test covering all eight meal-plan tables: ordering and cascade delete. */
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
    private MealPlanStatRepository mealPlanStatRepository;

    @Autowired
    private MealPlanChangelogEntryRepository mealPlanChangelogEntryRepository;

    @Autowired
    private MealPlanSourceRepository mealPlanSourceRepository;

    @Autowired
    private MealPlanSectionRepository mealPlanSectionRepository;

    @Autowired
    private MealPlanRowRepository mealPlanRowRepository;

    @Autowired
    private MealPlanShoppingCategoryRepository mealPlanShoppingCategoryRepository;

    @Autowired
    private MealPlanShoppingItemRepository mealPlanShoppingItemRepository;

    private MealPlanEntity mealPlan;

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
     * Creates the singleton meal-plan header row used as the foreign-key target for this test's children.
     */
    @BeforeEach
    void setUp() {
        final MealPlanEntity newMealPlan = new MealPlanEntity();
        newMealPlan.setId(MealPlanEntity.SINGLETON_ID);
        newMealPlan.setEyebrow("Version 3");
        newMealPlan.setTitle("Ernährungsplan & Einkaufsliste");
        newMealPlan.setDescription("description");
        newMealPlan.setShoppingListTitle("shopping list title");
        newMealPlan.setShoppingListNote("shopping list note");
        newMealPlan.setShoppingListCallout(null);
        newMealPlan.setFooterNote("footer note");
        mealPlan = mealPlanRepository.save(newMealPlan);
    }

    /**
     * Removes the meal-plan row created for this test, cascading to any remaining children.
     */
    @AfterEach
    void tearDown() {
        mealPlanRepository.deleteById(mealPlan.getId());
    }

    @Test
    void savesAndFindsStatsOrderedBySortOrder() {
        saveStat("Protein", "~184 g", 1);
        saveStat("Tagesbudget (Ø)", "2.416 kcal", 0);

        final List<MealPlanStatEntity> stats =
                mealPlanStatRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId());

        assertThat(stats).extracting(MealPlanStatEntity::getLabel)
                .containsExactly("Tagesbudget (Ø)", "Protein");
    }

    @Test
    void savesAndFindsChangelogEntriesOrderedBySortOrder() {
        saveChangelogEntry("Magerquark", null, "neu: 300 g/Tag", 1);
        saveChangelogEntry("Whey", "80 g/Tag", "→ 40 g/Tag", 0);

        final List<MealPlanChangelogEntryEntity> entries =
                mealPlanChangelogEntryRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId());

        assertThat(entries).extracting(MealPlanChangelogEntryEntity::getTag)
                .containsExactly("Whey", "Magerquark");
        assertThat(entries.get(0).getWas()).isEqualTo("80 g/Tag");
        assertThat(entries.get(1).getWas()).isNull();
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
    void savesAndFindsSectionsWithRowsOrderedBySortOrder() {
        final MealPlanSectionEntity section = saveSection("2 · Wochentage", "note", "Tagesgesamt", "2.407 kcal", "182,2 g", 0);

        saveRow(section.getId(), "Abendessen", "details", "qty", "923", "73,9 g", 1);
        saveRow(section.getId(), "Frühstück", "details", "qty", "519", "28,0 g", 0);

        final List<MealPlanSectionEntity> sections =
                mealPlanSectionRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId());
        assertThat(sections).hasSize(1);
        assertThat(sections.getFirst().getTotalsLabel()).isEqualTo("Tagesgesamt");

        final List<MealPlanRowEntity> rows =
                mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(section.getId());
        assertThat(rows).extracting(MealPlanRowEntity::getMeal)
                .containsExactly("Frühstück", "Abendessen");
    }

    @Test
    void savesAndFindsShoppingCategoriesWithItemsOrderedBySortOrder() {
        final MealPlanShoppingCategoryEntity category = saveShoppingCategory("Fleisch & Fisch", 0);

        saveShoppingItem(category.getId(), "Rinderhüftsteak", "Lidl", null, null, "435 g", 1);
        saveShoppingItem(category.getId(), "Hähnchenbrustfilet", "frisch, Kühltheke", "warn", "nur 1.200 g verfügbar", "1.200 g", 0);

        final List<MealPlanShoppingCategoryEntity> categories =
                mealPlanShoppingCategoryRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId());
        assertThat(categories).hasSize(1);

        final List<MealPlanShoppingItemEntity> items = mealPlanShoppingItemRepository
                .findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(category.getId());
        assertThat(items).extracting(MealPlanShoppingItemEntity::getName)
                .containsExactly("Hähnchenbrustfilet", "Rinderhüftsteak");
        assertThat(items.getFirst().getBadge()).isEqualTo("warn");
        assertThat(items.get(1).getBadge()).isNull();
    }

    @Test
    void deletingMealPlanCascadesToAllChildTables() {
        saveStat("Protein", "~184 g", 0);
        saveChangelogEntry("Whey", null, "text", 0);
        saveSource("Magerquark", "https://example.org/magerquark", 0);
        final MealPlanSectionEntity section = saveSection("Section", "note", null, null, null, 0);
        saveRow(section.getId(), "Frühstück", "details", "qty", "519", "28,0 g", 0);
        final MealPlanShoppingCategoryEntity category = saveShoppingCategory("Fleisch & Fisch", 0);
        saveShoppingItem(category.getId(), "Hähnchenbrustfilet", null, null, null, "1.200 g", 0);

        mealPlanRepository.deleteById(mealPlan.getId());
        mealPlanRepository.flush();

        assertThat(mealPlanStatRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId())).isEmpty();
        assertThat(mealPlanChangelogEntryRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId())).isEmpty();
        assertThat(mealPlanSourceRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId())).isEmpty();
        assertThat(mealPlanSectionRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId())).isEmpty();
        assertThat(mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(section.getId())).isEmpty();
        assertThat(mealPlanShoppingCategoryRepository.findAllByMealPlanIdOrderBySortOrderAsc(mealPlan.getId())).isEmpty();
        assertThat(mealPlanShoppingItemRepository
                .findAllByMealPlanShoppingCategoryIdOrderBySortOrderAsc(category.getId())).isEmpty();
    }

    private MealPlanStatEntity saveStat(String label, String value, int sortOrder) {
        final MealPlanStatEntity stat = new MealPlanStatEntity();
        stat.setMealPlanId(mealPlan.getId());
        stat.setLabel(label);
        stat.setValue(value);
        stat.setSortOrder(sortOrder);
        return mealPlanStatRepository.save(stat);
    }

    private MealPlanChangelogEntryEntity saveChangelogEntry(String tag, String was, String text, int sortOrder) {
        final MealPlanChangelogEntryEntity entry = new MealPlanChangelogEntryEntity();
        entry.setMealPlanId(mealPlan.getId());
        entry.setTag(tag);
        entry.setWas(was);
        entry.setText(text);
        entry.setSortOrder(sortOrder);
        return mealPlanChangelogEntryRepository.save(entry);
    }

    private MealPlanSourceEntity saveSource(String label, String url, int sortOrder) {
        final MealPlanSourceEntity source = new MealPlanSourceEntity();
        source.setMealPlanId(mealPlan.getId());
        source.setLabel(label);
        source.setUrl(url);
        source.setSortOrder(sortOrder);
        return mealPlanSourceRepository.save(source);
    }

    private MealPlanSectionEntity saveSection(
            String title, String note, String totalsLabel, String totalsKcal, String totalsProtein, int sortOrder) {
        final MealPlanSectionEntity section = new MealPlanSectionEntity();
        section.setMealPlanId(mealPlan.getId());
        section.setTitle(title);
        section.setNote(note);
        section.setTotalsLabel(totalsLabel);
        section.setTotalsKcal(totalsKcal);
        section.setTotalsProtein(totalsProtein);
        section.setSortOrder(sortOrder);
        return mealPlanSectionRepository.save(section);
    }

    private MealPlanRowEntity saveRow(
            UUID sectionId, String meal, String details, String qty, String kcal, String protein, int sortOrder) {
        final MealPlanRowEntity row = new MealPlanRowEntity();
        row.setMealPlanSectionId(sectionId);
        row.setMeal(meal);
        row.setDetails(details);
        row.setQty(qty);
        row.setKcal(kcal);
        row.setProtein(protein);
        row.setSortOrder(sortOrder);
        return mealPlanRowRepository.save(row);
    }

    private MealPlanShoppingCategoryEntity saveShoppingCategory(String title, int sortOrder) {
        final MealPlanShoppingCategoryEntity category = new MealPlanShoppingCategoryEntity();
        category.setMealPlanId(mealPlan.getId());
        category.setTitle(title);
        category.setSortOrder(sortOrder);
        return mealPlanShoppingCategoryRepository.save(category);
    }

    private MealPlanShoppingItemEntity saveShoppingItem(
            UUID categoryId, String name, String brand, String badge, String badgeText, String qty, int sortOrder) {
        final MealPlanShoppingItemEntity item = new MealPlanShoppingItemEntity();
        item.setMealPlanShoppingCategoryId(categoryId);
        item.setName(name);
        item.setBrand(brand);
        item.setBadge(badge);
        item.setBadgeText(badgeText);
        item.setQty(qty);
        item.setSortOrder(sortOrder);
        return mealPlanShoppingItemRepository.save(item);
    }
}
