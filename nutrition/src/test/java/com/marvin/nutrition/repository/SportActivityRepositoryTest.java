package com.marvin.nutrition.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.nutrition.entity.SportActivityEntity;
import com.marvin.nutrition.entity.SportActivityType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

/** Repository integration test for {@link SportActivityEntity}. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SportActivityRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private SportActivityRepository sportActivityRepository;

    /**
     * Registers dynamic Flyway/datasource properties so migrations run against the Testcontainers database.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Test
    void findByEntryDateOrderByCreationDateAscReturnsActivitiesInCreationOrder() throws InterruptedException {
        final LocalDate entryDate = LocalDate.of(2026, 6, 1);

        final SportActivityEntity first = newActivity(entryDate, SportActivityType.RUNNING, "Morning run");
        sportActivityRepository.save(first);
        sportActivityRepository.flush();
        Thread.sleep(10);

        final SportActivityEntity second = newActivity(entryDate, SportActivityType.SWIMMING, "Pool laps");
        sportActivityRepository.save(second);
        sportActivityRepository.flush();
        Thread.sleep(10);

        final SportActivityEntity third = newActivity(entryDate, SportActivityType.CYCLING, "Evening ride");
        sportActivityRepository.save(third);
        sportActivityRepository.flush();

        final List<SportActivityEntity> activities = sportActivityRepository.findByEntryDateOrderByCreationDateAsc(entryDate);

        assertThat(activities).extracting(SportActivityEntity::getDescription)
                .containsExactly("Morning run", "Pool laps", "Evening ride");
    }

    @Test
    void findByEntryDateBetweenOrderByEntryDateAscCreationDateAscReturnsOnlyInRangeOrdered() {
        final LocalDate before = LocalDate.of(2026, 5, 30);
        final LocalDate from = LocalDate.of(2026, 6, 1);
        final LocalDate middle = LocalDate.of(2026, 6, 2);
        final LocalDate to = LocalDate.of(2026, 6, 3);
        final LocalDate after = LocalDate.of(2026, 6, 5);

        sportActivityRepository.save(newActivity(before, SportActivityType.WALKING, "Outside range before"));
        sportActivityRepository.save(newActivity(from, SportActivityType.RUNNING, "Day one"));
        sportActivityRepository.save(newActivity(middle, SportActivityType.STRENGTH_TRAINING, "Day two"));
        sportActivityRepository.save(newActivity(to, SportActivityType.OTHER, "Day three"));
        sportActivityRepository.save(newActivity(after, SportActivityType.CYCLING, "Outside range after"));

        final List<SportActivityEntity> activities =
                sportActivityRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to);

        assertThat(activities).extracting(SportActivityEntity::getDescription)
                .containsExactly("Day one", "Day two", "Day three");
    }

    private SportActivityEntity newActivity(final LocalDate entryDate, final SportActivityType activityType, final String description) {
        final SportActivityEntity activity = new SportActivityEntity();
        activity.setEntryDate(entryDate);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setKcalBurned(BigDecimal.valueOf(300));
        return activity;
    }
}
