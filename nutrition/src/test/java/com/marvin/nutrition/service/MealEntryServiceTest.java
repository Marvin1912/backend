package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealEntryRequest;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.dto.UpdateMealEntryRequest;
import com.marvin.nutrition.entity.DayTargetSnapshotEntity;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealEntryEntity;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.mapper.MealEntryMapper;
import com.marvin.nutrition.repository.DayTargetSnapshotRepository;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link MealEntryService} covering CRUD delegation and day summary computation. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealEntryService Tests")
class MealEntryServiceTest {

    @Mock
    private MealEntryRepository mealEntryRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private MealEntryMapper mealEntryMapper;

    @Mock
    private NutritionTargetService nutritionTargetService;

    @Mock
    private DayTargetSnapshotRepository dayTargetSnapshotRepository;

    @Mock
    private MealEntryWriteService mealEntryWriteService;

    @InjectMocks
    private MealEntryService mealEntryService;

    private UUID foodId;
    private UUID entryId;
    private MealEntryDTO mealEntryDTO;
    private LocalDate today;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        foodId = UUID.randomUUID();
        entryId = UUID.randomUUID();
        today = LocalDate.of(2026, 6, 7);

        mealEntryDTO = new MealEntryDTO(
                entryId, today, MealType.LUNCH, foodId, null,
                new BigDecimal("150.00"),
                new BigDecimal("300.00"), new BigDecimal("30.00"),
                new BigDecimal("15.00"), new BigDecimal("7.50"), "Test Food"
        );
    }

    // -----------------------------------------------------------------------
    // addEntry — delegation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntry delegates to MealEntryWriteService and returns its result")
    void addEntry_DelegatesToMealEntryWriteService() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(mealEntryWriteService.createEntry(today, req)).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectNext(mealEntryDTO)
                .verifyComplete();

        verify(mealEntryWriteService).createEntry(today, req);
    }

    // -----------------------------------------------------------------------
    // updateEntry — delegation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateEntry delegates to MealEntryWriteService and returns its result")
    void updateEntry_DelegatesToMealEntryWriteService() {
        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                MealType.DINNER, null, null, null, null, null, null
        );

        when(mealEntryWriteService.updateEntry(entryId, req)).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.updateEntry(entryId, req))
                .expectNext(mealEntryDTO)
                .verifyComplete();

        verify(mealEntryWriteService).updateEntry(entryId, req);
    }

    // -----------------------------------------------------------------------
    // deleteEntry — delegation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteEntry delegates to MealEntryWriteService and completes")
    void deleteEntry_DelegatesToMealEntryWriteService() {
        StepVerifier.create(mealEntryService.deleteEntry(entryId))
                .verifyComplete();

        verify(mealEntryWriteService).deleteEntry(entryId);
    }

    // -----------------------------------------------------------------------
    // getDay
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getDay computes correct totals and remaining when targets are available")
    void getDay_WithTargets_ComputesCorrectTotalsAndRemaining() {
        final MealEntryEntity entry1 = new MealEntryEntity();
        entry1.setKcal(new BigDecimal("400.00"));
        entry1.setProteinG(new BigDecimal("30.00"));
        entry1.setCarbsG(new BigDecimal("50.00"));
        entry1.setFatG(new BigDecimal("10.00"));

        final MealEntryEntity entry2 = new MealEntryEntity();
        entry2.setKcal(new BigDecimal("200.00"));
        entry2.setProteinG(new BigDecimal("10.00"));
        entry2.setCarbsG(new BigDecimal("25.00"));
        entry2.setFatG(new BigDecimal("5.00"));

        final MealEntryDTO dto1 = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.BREAKFAST, null, "Oats", null,
                new BigDecimal("400.00"), new BigDecimal("30.00"),
                new BigDecimal("50.00"), new BigDecimal("10.00"), null
        );
        final MealEntryDTO dto2 = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.LUNCH, null, "Salad", null,
                new BigDecimal("200.00"), new BigDecimal("10.00"),
                new BigDecimal("25.00"), new BigDecimal("5.00"), null
        );

        final TargetsDTO targets = new TargetsDTO(1700, 2200, 2000, 150, 67, 248, "MIFFLIN_ST_JEOR");

        when(mealEntryRepository.findByEntryDateOrderByCreationDateAsc(today))
                .thenReturn(List.of(entry1, entry2));
        when(foodRepository.findAllById(any())).thenReturn(List.of());
        when(mealEntryMapper.toDTO(any(MealEntryEntity.class), isNull()))
                .thenReturn(dto1, dto2);
        when(nutritionTargetService.getTargets()).thenReturn(Mono.just(targets));

        StepVerifier.create(mealEntryService.getDay(today))
                .assertNext(summary -> {
                    assert today.equals(summary.date());
                    assert 2 == summary.entries().size();
                    // totals: kcal=600, protein=40, carbs=75, fat=15
                    assert new BigDecimal("600.00").compareTo(summary.totals().kcal()) == 0;
                    assert new BigDecimal("40.00").compareTo(summary.totals().proteinG()) == 0;
                    assert new BigDecimal("75.00").compareTo(summary.totals().carbsG()) == 0;
                    assert new BigDecimal("15.00").compareTo(summary.totals().fatG()) == 0;
                    // targets present
                    assert summary.targets() != null;
                    assert 2000 == summary.targets().targetKcal();
                    // remaining: kcal = 2000 - 600 = 1400
                    assert summary.remaining() != null;
                    assert new BigDecimal("1400").compareTo(summary.remaining().kcal()) == 0;
                    assert new BigDecimal("110").compareTo(summary.remaining().proteinG()) == 0;
                    assert new BigDecimal("173").compareTo(summary.remaining().carbsG()) == 0;
                    assert new BigDecimal("52").compareTo(summary.remaining().fatG()) == 0;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getDay returns null targets and remaining when NutritionTargetService errors with TargetCalculationException")
    void getDay_TargetCalculationException_ReturnsNullTargetsAndRemaining() {
        when(mealEntryRepository.findByEntryDateOrderByCreationDateAsc(today))
                .thenReturn(List.of());
        when(foodRepository.findAllById(List.of())).thenReturn(List.of());
        when(nutritionTargetService.getTargets())
                .thenReturn(Mono.error(new TargetCalculationException("No profile")));

        StepVerifier.create(mealEntryService.getDay(today))
                .assertNext(summary -> {
                    assert summary.targets() == null;
                    assert summary.remaining() == null;
                    assert new BigDecimal("0").compareTo(summary.totals().kcal()) == 0;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getDay returns zero totals for an empty day")
    void getDay_EmptyDay_ReturnsZeroTotals() {
        when(mealEntryRepository.findByEntryDateOrderByCreationDateAsc(today))
                .thenReturn(List.of());
        when(foodRepository.findAllById(List.of())).thenReturn(List.of());
        when(nutritionTargetService.getTargets())
                .thenReturn(Mono.error(new TargetCalculationException("No data")));

        StepVerifier.create(mealEntryService.getDay(today))
                .assertNext(summary -> {
                    assert BigDecimal.ZERO.compareTo(summary.totals().kcal()) == 0;
                    assert BigDecimal.ZERO.compareTo(summary.totals().proteinG()) == 0;
                    assert BigDecimal.ZERO.compareTo(summary.totals().carbsG()) == 0;
                    assert BigDecimal.ZERO.compareTo(summary.totals().fatG()) == 0;
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // getDay — historical day target snapshot
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getDay uses the persisted snapshot's targets for a historical date, regardless of live targets")
    void getDay_WithSnapshot_UsesSnapshotTargetsRegardlessOfLiveTargets() {
        final MealEntryEntity entry = new MealEntryEntity();
        entry.setKcal(new BigDecimal("400.00"));
        entry.setProteinG(new BigDecimal("30.00"));
        entry.setCarbsG(new BigDecimal("50.00"));
        entry.setFatG(new BigDecimal("10.00"));

        final MealEntryDTO dto = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.BREAKFAST, null, "Oats", null,
                new BigDecimal("400.00"), new BigDecimal("30.00"),
                new BigDecimal("50.00"), new BigDecimal("10.00"), null
        );

        final DayTargetSnapshotEntity snapshot = new DayTargetSnapshotEntity();
        snapshot.setEntryDate(today);
        snapshot.setBmr(1700);
        snapshot.setMaintenanceKcal(2100);
        snapshot.setTargetKcal(2000);
        snapshot.setProteinG(150);
        snapshot.setFatG(67);
        snapshot.setCarbsG(248);
        snapshot.setBasis("MIFFLIN_ST_JEOR");

        when(mealEntryRepository.findByEntryDateOrderByCreationDateAsc(today))
                .thenReturn(List.of(entry));
        when(foodRepository.findAllById(any())).thenReturn(List.of());
        when(mealEntryMapper.toDTO(any(MealEntryEntity.class), isNull())).thenReturn(dto);
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.of(snapshot));

        StepVerifier.create(mealEntryService.getDay(today))
                .assertNext(summary -> {
                    assertEquals(2000, summary.targets().targetKcal());
                    assertEquals(150, summary.targets().proteinG());
                    assertEquals(67, summary.targets().fatG());
                    assertEquals(248, summary.targets().carbsG());
                    assertEquals(1700, summary.targets().bmr());
                    assertEquals(2100, summary.targets().maintenanceKcal());
                    assertEquals("MIFFLIN_ST_JEOR", summary.targets().basis());
                    // remaining = snapshot targetKcal - totals.kcal = 2000 - 400 = 1600
                    assertEquals(0, new BigDecimal("1600").compareTo(summary.remaining().kcal()));
                })
                .verifyComplete();

        verify(nutritionTargetService, never()).getTargets();
    }

    @Test
    @DisplayName("getDay falls back to live targets when no snapshot exists for the date")
    void getDay_NoSnapshot_FallsBackToLiveTargets() {
        final TargetsDTO liveTargets = new TargetsDTO(1750, 2160, 2160, 160, 72, 252, "MIFFLIN_ST_JEOR");

        when(mealEntryRepository.findByEntryDateOrderByCreationDateAsc(today))
                .thenReturn(List.of());
        when(foodRepository.findAllById(List.of())).thenReturn(List.of());
        when(dayTargetSnapshotRepository.findById(today)).thenReturn(Optional.empty());
        when(nutritionTargetService.getTargets()).thenReturn(Mono.just(liveTargets));

        StepVerifier.create(mealEntryService.getDay(today))
                .assertNext(summary -> {
                    assertEquals(2160, summary.targets().targetKcal());
                    assertEquals(160, summary.targets().proteinG());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // getDay — foodName population
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getDay falls back to snapshotted foodName for an orphaned entry whose food was deleted")
    void getDay_OrphanedEntry_FallsBackToSnapshottedFoodName() {
        final MealEntryEntity orphanedEntry = new MealEntryEntity();
        orphanedEntry.setFoodId(null);
        orphanedEntry.setFoodName("Deleted Food");
        orphanedEntry.setKcal(new BigDecimal("300.00"));
        orphanedEntry.setProteinG(new BigDecimal("20.00"));
        orphanedEntry.setCarbsG(new BigDecimal("30.00"));
        orphanedEntry.setFatG(new BigDecimal("10.00"));

        final MealEntryDTO orphanedDTO = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.LUNCH, null, null,
                new BigDecimal("150.00"),
                new BigDecimal("300.00"), new BigDecimal("20.00"),
                new BigDecimal("30.00"), new BigDecimal("10.00"), "Deleted Food"
        );

        when(mealEntryRepository.findByEntryDateOrderByCreationDateAsc(today))
                .thenReturn(List.of(orphanedEntry));
        when(foodRepository.findAllById(List.of())).thenReturn(List.of());
        when(mealEntryMapper.toDTO(orphanedEntry, "Deleted Food")).thenReturn(orphanedDTO);
        when(nutritionTargetService.getTargets())
                .thenReturn(Mono.error(new TargetCalculationException("No profile")));

        StepVerifier.create(mealEntryService.getDay(today))
                .assertNext(summary -> {
                    assertEquals(1, summary.entries().size());
                    assertEquals("Deleted Food", summary.entries().get(0).foodName());
                })
                .verifyComplete();

        verify(foodRepository).findAllById(List.of());
    }

    @Test
    @DisplayName("getDay resolves foodName via batch lookup for food-backed entries")
    void getDay_FoodBackedEntries_ResolvesFoodNameViaBatchLookup() {
        final UUID foodId2 = UUID.randomUUID();

        final FoodEntity foodEntity1 = new FoodEntity();
        foodEntity1.setId(foodId);
        foodEntity1.setName("Test Food");

        final FoodEntity food2 = new FoodEntity();
        food2.setId(foodId2);
        food2.setName("Brown Rice");

        final MealEntryEntity foodEntry1 = new MealEntryEntity();
        foodEntry1.setFoodId(foodId);
        foodEntry1.setKcal(new BigDecimal("300.00"));
        foodEntry1.setProteinG(new BigDecimal("20.00"));
        foodEntry1.setCarbsG(new BigDecimal("30.00"));
        foodEntry1.setFatG(new BigDecimal("10.00"));

        final MealEntryEntity foodEntry2 = new MealEntryEntity();
        foodEntry2.setFoodId(foodId2);
        foodEntry2.setKcal(new BigDecimal("200.00"));
        foodEntry2.setProteinG(new BigDecimal("5.00"));
        foodEntry2.setCarbsG(new BigDecimal("40.00"));
        foodEntry2.setFatG(new BigDecimal("2.00"));

        final MealEntryDTO dtoWithFoodName1 = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.LUNCH, foodId, null,
                new BigDecimal("150.00"),
                new BigDecimal("300.00"), new BigDecimal("20.00"),
                new BigDecimal("30.00"), new BigDecimal("10.00"), "Test Food"
        );
        final MealEntryDTO dtoWithFoodName2 = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.DINNER, foodId2, null,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"), new BigDecimal("5.00"),
                new BigDecimal("40.00"), new BigDecimal("2.00"), "Brown Rice"
        );

        when(mealEntryRepository.findByEntryDateOrderByCreationDateAsc(today))
                .thenReturn(List.of(foodEntry1, foodEntry2));
        when(foodRepository.findAllById(any())).thenReturn(List.of(foodEntity1, food2));
        when(mealEntryMapper.toDTO(foodEntry1, "Test Food")).thenReturn(dtoWithFoodName1);
        when(mealEntryMapper.toDTO(foodEntry2, "Brown Rice")).thenReturn(dtoWithFoodName2);
        when(nutritionTargetService.getTargets())
                .thenReturn(Mono.error(new TargetCalculationException("No profile")));

        StepVerifier.create(mealEntryService.getDay(today))
                .assertNext(summary -> {
                    assertEquals(2, summary.entries().size());
                    assertEquals("Test Food", summary.entries().get(0).foodName());
                    assertEquals("Brown Rice", summary.entries().get(1).foodName());
                })
                .verifyComplete();

        verify(foodRepository).findAllById(any());
    }
}
