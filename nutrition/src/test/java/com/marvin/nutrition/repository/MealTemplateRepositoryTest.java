package com.marvin.nutrition.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.entity.MealTemplateEntity;
import com.marvin.nutrition.entity.MealTemplateItemEntity;
import java.math.BigDecimal;
import java.util.List;
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

/** Repository integration test for {@link MealTemplateEntity} and {@link MealTemplateItemEntity}. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MealTemplateRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private MealTemplateRepository mealTemplateRepository;

    @Autowired
    private MealTemplateItemRepository mealTemplateItemRepository;

    @Autowired
    private FoodRepository foodRepository;

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
     * Creates a food entity used as a foreign-key target for meal template items.
     */
    @BeforeEach
    void setUp() {
        final FoodEntity newFood = new FoodEntity();
        newFood.setName("Oatmeal");
        newFood.setKcalPer100(BigDecimal.valueOf(370));
        newFood.setProteinPer100(BigDecimal.valueOf(13));
        newFood.setCarbsPer100(BigDecimal.valueOf(60));
        newFood.setFatPer100(BigDecimal.valueOf(7));
        newFood.setSource(FoodSource.MANUAL);
        food = foodRepository.save(newFood);
    }

    /**
     * Removes the food entity created for this test.
     */
    @AfterEach
    void tearDown() {
        foodRepository.delete(food);
    }

    @Test
    void savesAndFindsTemplateWithItemsOrderedByName() {
        final MealTemplateEntity templateB = new MealTemplateEntity();
        templateB.setName("Breakfast B");
        final MealTemplateEntity savedB = mealTemplateRepository.save(templateB);

        final MealTemplateEntity templateA = new MealTemplateEntity();
        templateA.setName("Breakfast A");
        final MealTemplateEntity savedA = mealTemplateRepository.save(templateA);

        final MealTemplateItemEntity item = new MealTemplateItemEntity();
        item.setMealTemplateId(savedA.getId());
        item.setFoodId(food.getId());
        item.setQuantityG(BigDecimal.valueOf(50));
        mealTemplateItemRepository.save(item);

        final List<MealTemplateEntity> templates = mealTemplateRepository.findAllByOrderByNameAsc();

        assertThat(templates).extracting(MealTemplateEntity::getName)
                .containsExactly("Breakfast A", "Breakfast B");

        final List<MealTemplateItemEntity> items = mealTemplateItemRepository.findByMealTemplateId(savedA.getId());
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getFoodId()).isEqualTo(food.getId());
        assertThat(items.getFirst().getQuantityG()).isEqualByComparingTo(BigDecimal.valueOf(50));

        mealTemplateRepository.delete(savedB);
    }

    @Test
    void deletingTemplateCascadesToItsItems() {
        final MealTemplateEntity template = new MealTemplateEntity();
        template.setName("Dinner");
        final MealTemplateEntity savedTemplate = mealTemplateRepository.save(template);

        final MealTemplateItemEntity item = new MealTemplateItemEntity();
        item.setMealTemplateId(savedTemplate.getId());
        item.setFoodId(food.getId());
        item.setQuantityG(BigDecimal.valueOf(120));
        mealTemplateItemRepository.save(item);

        mealTemplateRepository.delete(savedTemplate);
        mealTemplateRepository.flush();

        assertThat(mealTemplateItemRepository.findByMealTemplateId(savedTemplate.getId())).isEmpty();
    }

    @Test
    void countByFoodIdCountsItemsReferencingTheFood() {
        final MealTemplateEntity template = new MealTemplateEntity();
        template.setName("Snack");
        final MealTemplateEntity savedTemplate = mealTemplateRepository.save(template);

        final MealTemplateItemEntity item = new MealTemplateItemEntity();
        item.setMealTemplateId(savedTemplate.getId());
        item.setFoodId(food.getId());
        item.setQuantityG(BigDecimal.valueOf(30));
        mealTemplateItemRepository.save(item);

        assertThat(mealTemplateItemRepository.countByFoodId(food.getId())).isEqualTo(1);

        mealTemplateRepository.delete(savedTemplate);
    }

    @Test
    void deleteByMealTemplateIdRemovesOnlyMatchingItems() {
        final MealTemplateEntity template = new MealTemplateEntity();
        template.setName("Lunch");
        final MealTemplateEntity savedTemplate = mealTemplateRepository.save(template);

        final MealTemplateItemEntity item = new MealTemplateItemEntity();
        item.setMealTemplateId(savedTemplate.getId());
        item.setFoodId(food.getId());
        item.setQuantityG(BigDecimal.valueOf(75));
        mealTemplateItemRepository.save(item);

        mealTemplateItemRepository.deleteByMealTemplateId(savedTemplate.getId());

        assertThat(mealTemplateItemRepository.findByMealTemplateId(savedTemplate.getId())).isEmpty();

        mealTemplateRepository.delete(savedTemplate);
    }
}
