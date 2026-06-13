package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link MealEntryService} covering all business logic paths. */
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
    private DayTargetSnapshotService dayTargetSnapshotService;

    @InjectMocks
    private MealEntryService mealEntryService;

    private UUID foodId;
    private UUID entryId;
    private FoodEntity foodEntity;
    private MealEntryEntity mealEntryEntity;
    private MealEntryDTO mealEntryDTO;
    private LocalDate today;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        foodId = UUID.randomUUID();
        entryId = UUID.randomUUID();
        today = LocalDate.of(2026, 6, 7);

        foodEntity = new FoodEntity();
        foodEntity.setName("Test Food");
        foodEntity.setKcalPer100(new BigDecimal("200.00"));
        foodEntity.setProteinPer100(new BigDecimal("20.00"));
        foodEntity.setCarbsPer100(new BigDecimal("10.00"));
        foodEntity.setFatPer100(new BigDecimal("5.00"));

        mealEntryEntity = new MealEntryEntity();
        mealEntryEntity.setEntryDate(today);
        mealEntryEntity.setMealType(MealType.LUNCH);
        mealEntryEntity.setKcal(new BigDecimal("300.00"));
        mealEntryEntity.setProteinG(new BigDecimal("30.00"));
        mealEntryEntity.setCarbsG(new BigDecimal("15.00"));
        mealEntryEntity.setFatG(new BigDecimal("7.50"));

        mealEntryDTO = new MealEntryDTO(
                entryId, today, MealType.LUNCH, foodId, null,
                new BigDecimal("150.00"),
                new BigDecimal("300.00"), new BigDecimal("30.00"),
                new BigDecimal("15.00"), new BigDecimal("7.50"), "Test Food"
        );
    }

    // -----------------------------------------------------------------------
    // addEntry — food-backed snapshot correctness
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntry snapshots macros correctly from food × quantityG (kcalPer100=200, qty=150 → kcal=300.00)")
    void addEntry_FoodEntry_SnapshotsMacrosCorrectly() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectNext(mealEntryDTO)
                .verifyComplete();

        verify(mealEntryRepository).save(argThat(e ->
                new BigDecimal("300.00").compareTo(e.getKcal()) == 0
                        && new BigDecimal("30.00").compareTo(e.getProteinG()) == 0
                        && new BigDecimal("15.00").compareTo(e.getCarbsG()) == 0
                        && new BigDecimal("7.50").compareTo(e.getFatG()) == 0
                        && new BigDecimal("150").compareTo(e.getQuantityG()) == 0
                        && foodId.equals(e.getFoodId())
        ));
    }

    @Test
    @DisplayName("addEntry food-backed entry propagates food id and quantity to saved entity")
    void addEntry_FoodEntry_PropagatesFoodIdAndQuantity() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.BREAKFAST, foodId, new BigDecimal("100"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectNextCount(1)
                .verifyComplete();

        verify(mealEntryRepository).save(argThat(e ->
                foodId.equals(e.getFoodId()) && new BigDecimal("100").compareTo(e.getQuantityG()) == 0
        ));
    }

    // -----------------------------------------------------------------------
    // addEntry — ad-hoc entry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntry persists supplied macros for ad-hoc entry (no foodId)")
    void addEntry_AdHocEntry_PersistsSuppliedMacros() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.SNACK, null, null, "Homemade soup",
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00")
        );

        final MealEntryEntity saved = new MealEntryEntity();
        saved.setDescription("Homemade soup");
        saved.setKcal(new BigDecimal("250.00"));
        saved.setProteinG(new BigDecimal("15.00"));
        saved.setCarbsG(new BigDecimal("30.00"));
        saved.setFatG(new BigDecimal("8.00"));

        final MealEntryDTO adHocDTO = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.SNACK, null, "Homemade soup", null,
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00"), null
        );

        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(saved);
        when(mealEntryMapper.toDTO(saved)).thenReturn(adHocDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectNext(adHocDTO)
                .verifyComplete();

        verify(mealEntryRepository).save(argThat(e ->
                e.getFoodId() == null
                        && e.getQuantityG() == null
                        && "Homemade soup".equals(e.getDescription())
                        && new BigDecimal("250.00").compareTo(e.getKcal()) == 0
        ));
        verify(foodRepository, never()).findById(any());
    }

    // -----------------------------------------------------------------------
    // addEntry — error paths
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntry emits NoSuchElementException when food is not found")
    void addEntry_FoodNotFound_EmitsNoSuchElementException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.empty());

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("addEntry emits IllegalArgumentException when food entry has no quantityG")
    void addEntry_FoodEntryMissingQuantityG_EmitsIllegalArgumentException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, null, null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("addEntry emits IllegalArgumentException when ad-hoc entry has no description")
    void addEntry_AdHocMissingDescription_EmitsIllegalArgumentException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.SNACK, null, null, null,
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00")
        );

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("addEntry emits IllegalArgumentException when ad-hoc entry has no macros")
    void addEntry_AdHocMissingMacros_EmitsIllegalArgumentException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.SNACK, null, null, "Some food", null, null, null, null
        );

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(mealEntryRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // addEntry — day target snapshot persistence
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntry delegates day-target snapshot creation to DayTargetSnapshotService")
    void addEntry_DelegatesSnapshotCreationToDayTargetSnapshotService() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.SNACK, null, null, "Homemade soup",
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00")
        );

        final MealEntryEntity saved = new MealEntryEntity();
        saved.setDescription("Homemade soup");
        saved.setKcal(new BigDecimal("250.00"));
        saved.setProteinG(new BigDecimal("15.00"));
        saved.setCarbsG(new BigDecimal("30.00"));
        saved.setFatG(new BigDecimal("8.00"));

        final MealEntryDTO adHocDTO = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.SNACK, null, "Homemade soup", null,
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00"), null
        );

        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(saved);
        when(mealEntryMapper.toDTO(saved)).thenReturn(adHocDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectNext(adHocDTO)
                .verifyComplete();

        verify(dayTargetSnapshotService).ensureSnapshot(today);
    }

    // -----------------------------------------------------------------------
    // updateEntry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateEntry re-snapshots macros when food entry quantity changes")
    void updateEntry_FoodEntryQuantityChanged_ReSnapshotsMacros() {
        mealEntryEntity.setFoodId(foodId);
        mealEntryEntity.setQuantityG(new BigDecimal("150"));
        mealEntryEntity.setKcal(new BigDecimal("300.00"));

        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                null, new BigDecimal("200"), null, null, null, null, null
        );

        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.of(mealEntryEntity));
        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.updateEntry(entryId, req))
                .expectNextCount(1)
                .verifyComplete();

        // kcalPer100=200, qty=200 → kcal = 200*200/100 = 400.00
        verify(mealEntryRepository).save(argThat(e ->
                new BigDecimal("400.00").compareTo(e.getKcal()) == 0
                        && new BigDecimal("40.00").compareTo(e.getProteinG()) == 0
                        && new BigDecimal("20.00").compareTo(e.getCarbsG()) == 0
                        && new BigDecimal("10.00").compareTo(e.getFatG()) == 0
                        && new BigDecimal("200").compareTo(e.getQuantityG()) == 0
        ));
    }

    @Test
    @DisplayName("updateEntry applies non-null macro values for ad-hoc entry")
    void updateEntry_AdHocEntry_AppliesNonNullMacros() {
        mealEntryEntity.setFoodId(null);
        mealEntryEntity.setDescription("Old soup");
        mealEntryEntity.setKcal(new BigDecimal("100.00"));

        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                MealType.DINNER, null, "New soup",
                new BigDecimal("350.00"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00")
        );

        final MealEntryEntity updatedEntity = new MealEntryEntity();
        updatedEntity.setDescription("New soup");
        updatedEntity.setMealType(MealType.DINNER);
        updatedEntity.setKcal(new BigDecimal("350.00"));

        final MealEntryDTO updatedDTO = new MealEntryDTO(
                entryId, today, MealType.DINNER, null, "New soup", null,
                new BigDecimal("350.00"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00"), null
        );

        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.of(mealEntryEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(updatedEntity);
        when(mealEntryMapper.toDTO(updatedEntity)).thenReturn(updatedDTO);

        StepVerifier.create(mealEntryService.updateEntry(entryId, req))
                .expectNext(updatedDTO)
                .verifyComplete();

        verify(mealEntryRepository).save(argThat(e ->
                MealType.DINNER.equals(e.getMealType())
                        && "New soup".equals(e.getDescription())
                        && new BigDecimal("350.00").compareTo(e.getKcal()) == 0
        ));
    }

    @Test
    @DisplayName("updateEntry emits NoSuchElementException when entry not found")
    void updateEntry_NotFound_EmitsNoSuchElementException() {
        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                MealType.DINNER, null, null, null, null, null, null
        );

        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        StepVerifier.create(mealEntryService.updateEntry(entryId, req))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(mealEntryRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // deleteEntry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteEntry emits NoSuchElementException when entry not found")
    void deleteEntry_NotFound_EmitsNoSuchElementException() {
        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        StepVerifier.create(mealEntryService.deleteEntry(entryId))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(mealEntryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteEntry deletes and completes when entry exists")
    void deleteEntry_Exists_DeletesAndCompletes() {
        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.of(mealEntryEntity));

        StepVerifier.create(mealEntryService.deleteEntry(entryId))
                .verifyComplete();

        verify(mealEntryRepository).delete(mealEntryEntity);
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
    // getDays
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getDays returns one summary per day in range with a single query per data source")
    void getDays_ReturnsOneSummaryPerDayInRange() {
        final LocalDate from = today.minusDays(2);
        final LocalDate to = today;

        final MealEntryEntity entry = new MealEntryEntity();
        entry.setEntryDate(today);
        entry.setKcal(new BigDecimal("400.00"));
        entry.setProteinG(new BigDecimal("30.00"));
        entry.setCarbsG(new BigDecimal("50.00"));
        entry.setFatG(new BigDecimal("10.00"));

        final MealEntryDTO dto = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.BREAKFAST, null, "Oats", null,
                new BigDecimal("400.00"), new BigDecimal("30.00"),
                new BigDecimal("50.00"), new BigDecimal("10.00"), null
        );

        final TargetsDTO liveTargets = new TargetsDTO(1750, 2160, 2160, 160, 72, 252, "MIFFLIN_ST_JEOR");

        when(mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to))
                .thenReturn(List.of(entry));
        when(foodRepository.findAllById(any())).thenReturn(List.of());
        when(mealEntryMapper.toDTO(any(MealEntryEntity.class), isNull())).thenReturn(dto);
        when(dayTargetSnapshotRepository.findByEntryDateBetween(from, to)).thenReturn(List.of());
        when(nutritionTargetService.getTargets()).thenReturn(Mono.just(liveTargets));

        StepVerifier.create(mealEntryService.getDays(from, to))
                .assertNext(summaries -> {
                    assertEquals(3, summaries.size());
                    assertEquals(from, summaries.get(0).date());
                    assertEquals(to, summaries.get(2).date());

                    // days without entries have zero totals but still get the live targets
                    assertEquals(0, BigDecimal.ZERO.compareTo(summaries.get(0).totals().kcal()));
                    assertEquals(2160, summaries.get(0).targets().targetKcal());

                    // the day with an entry has the entry reflected in its totals
                    assertEquals(1, summaries.get(2).entries().size());
                    assertEquals(0, new BigDecimal("400.00").compareTo(summaries.get(2).totals().kcal()));
                    assertEquals(2160, summaries.get(2).targets().targetKcal());
                })
                .verifyComplete();

        verify(mealEntryRepository).findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to);
        verify(dayTargetSnapshotRepository).findByEntryDateBetween(from, to);
        verify(nutritionTargetService).getTargets();
    }

    @Test
    @DisplayName("getDays uses a day's persisted snapshot targets instead of live targets")
    void getDays_WithSnapshot_UsesSnapshotTargetsForThatDay() {
        final LocalDate from = today.minusDays(1);
        final LocalDate to = today;

        final DayTargetSnapshotEntity snapshot = new DayTargetSnapshotEntity();
        snapshot.setEntryDate(from);
        snapshot.setBmr(1700);
        snapshot.setMaintenanceKcal(2100);
        snapshot.setTargetKcal(2000);
        snapshot.setProteinG(150);
        snapshot.setFatG(67);
        snapshot.setCarbsG(248);
        snapshot.setBasis("MIFFLIN_ST_JEOR");

        final TargetsDTO liveTargets = new TargetsDTO(1750, 2160, 2160, 160, 72, 252, "MIFFLIN_ST_JEOR");

        when(mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to))
                .thenReturn(List.of());
        when(foodRepository.findAllById(any())).thenReturn(List.of());
        when(dayTargetSnapshotRepository.findByEntryDateBetween(from, to)).thenReturn(List.of(snapshot));
        when(nutritionTargetService.getTargets()).thenReturn(Mono.just(liveTargets));

        StepVerifier.create(mealEntryService.getDays(from, to))
                .assertNext(summaries -> {
                    assertEquals(2000, summaries.get(0).targets().targetKcal());
                    assertEquals(2160, summaries.get(1).targets().targetKcal());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getDays returns null targets and remaining when no targets can be computed")
    void getDays_NoTargetsAvailable_ReturnsNullTargetsAndRemaining() {
        final LocalDate from = today;
        final LocalDate to = today;

        when(mealEntryRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to))
                .thenReturn(List.of());
        when(foodRepository.findAllById(any())).thenReturn(List.of());
        when(dayTargetSnapshotRepository.findByEntryDateBetween(from, to)).thenReturn(List.of());
        when(nutritionTargetService.getTargets())
                .thenReturn(Mono.error(new TargetCalculationException("No profile")));

        StepVerifier.create(mealEntryService.getDays(from, to))
                .assertNext(summaries -> {
                    assertEquals(1, summaries.size());
                    assertNull(summaries.get(0).targets());
                    assertNull(summaries.get(0).remaining());
                })
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // foodName population
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntry populates foodName from food catalog for food-backed entry")
    void addEntry_FoodEntry_PopulatesFoodName() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .assertNext(dto -> assertEquals("Test Food", dto.foodName()))
                .verifyComplete();
    }

    @Test
    @DisplayName("addEntry leaves foodName null for ad-hoc entry")
    void addEntry_AdHocEntry_FoodNameIsNull() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.SNACK, null, null, "Homemade soup",
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00")
        );

        final MealEntryEntity saved = new MealEntryEntity();
        saved.setDescription("Homemade soup");
        saved.setKcal(new BigDecimal("250.00"));
        saved.setProteinG(new BigDecimal("15.00"));
        saved.setCarbsG(new BigDecimal("30.00"));
        saved.setFatG(new BigDecimal("8.00"));

        final MealEntryDTO adHocDTO = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.SNACK, null, "Homemade soup", null,
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00"), null
        );

        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(saved);
        when(mealEntryMapper.toDTO(saved)).thenReturn(adHocDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .assertNext(dto -> assertNull(dto.foodName()))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateEntry populates foodName for food-backed entry")
    void updateEntry_FoodEntry_PopulatesFoodName() {
        mealEntryEntity.setFoodId(foodId);
        mealEntryEntity.setQuantityG(new BigDecimal("150"));

        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                null, new BigDecimal("150"), null, null, null, null, null
        );

        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.of(mealEntryEntity));
        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.updateEntry(entryId, req))
                .assertNext(dto -> assertEquals("Test Food", dto.foodName()))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateEntry leaves foodName null for ad-hoc entry")
    void updateEntry_AdHocEntry_FoodNameIsNull() {
        mealEntryEntity.setFoodId(null);
        mealEntryEntity.setDescription("Old soup");
        mealEntryEntity.setKcal(new BigDecimal("100.00"));

        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                null, null, "Updated soup", null, null, null, null
        );

        final MealEntryEntity savedEntity = new MealEntryEntity();
        savedEntity.setDescription("Updated soup");

        final MealEntryDTO adHocDTO = new MealEntryDTO(
                entryId, today, MealType.LUNCH, null, "Updated soup", null,
                new BigDecimal("100.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal("0.00"), null
        );

        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.of(mealEntryEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(savedEntity);
        when(mealEntryMapper.toDTO(savedEntity)).thenReturn(adHocDTO);

        StepVerifier.create(mealEntryService.updateEntry(entryId, req))
                .assertNext(dto -> assertNull(dto.foodName()))
                .verifyComplete();
    }

    @Test
    @DisplayName("addEntry snapshots foodName onto the saved entity for a food-backed entry")
    void addEntry_FoodEntry_SnapshotsFoodNameOntoSavedEntity() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.addEntry(today, req))
                .expectNextCount(1)
                .verifyComplete();

        verify(mealEntryRepository).save(argThat(e -> "Test Food".equals(e.getFoodName())));
    }

    @Test
    @DisplayName("updateEntry re-snapshots foodName onto the saved entity for a food-backed entry")
    void updateEntry_FoodEntry_ReSnapshotsFoodNameOntoSavedEntity() {
        mealEntryEntity.setFoodId(foodId);
        mealEntryEntity.setQuantityG(new BigDecimal("150"));

        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                null, new BigDecimal("200"), null, null, null, null, null
        );

        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.of(mealEntryEntity));
        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        StepVerifier.create(mealEntryService.updateEntry(entryId, req))
                .expectNextCount(1)
                .verifyComplete();

        verify(mealEntryRepository).save(argThat(e -> "Test Food".equals(e.getFoodName())));
    }

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

        final FoodEntity food2 = new FoodEntity();
        food2.setId(foodId2);
        food2.setName("Brown Rice");

        foodEntity.setId(foodId);

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
        when(foodRepository.findAllById(any())).thenReturn(List.of(foodEntity, food2));
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
