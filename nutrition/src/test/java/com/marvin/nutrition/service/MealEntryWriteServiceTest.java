package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealEntryRequest;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.UpdateMealEntryRequest;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.MealEntryEntity;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.mapper.MealEntryMapper;
import com.marvin.nutrition.repository.FoodRepository;
import com.marvin.nutrition.repository.MealEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/** Unit tests for {@link MealEntryWriteService} covering the transactional write operations. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealEntryWriteService Tests")
class MealEntryWriteServiceTest {

    @Mock
    private MealEntryRepository mealEntryRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private MealEntryMapper mealEntryMapper;

    @Mock
    private DayTargetSnapshotService dayTargetSnapshotService;

    @InjectMocks
    private MealEntryWriteService mealEntryWriteService;

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
    // createEntry — food-backed snapshot correctness
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createEntry snapshots macros correctly from food × quantityG (kcalPer100=200, qty=150 → kcal=300.00)")
    void createEntry_FoodEntry_SnapshotsMacrosCorrectly() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        final MealEntryDTO result = mealEntryWriteService.createEntry(today, req);

        assertEquals(mealEntryDTO, result);
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
    @DisplayName("createEntry food-backed entry propagates food id and quantity to saved entity")
    void createEntry_FoodEntry_PropagatesFoodIdAndQuantity() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.BREAKFAST, foodId, new BigDecimal("100"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        mealEntryWriteService.createEntry(today, req);

        verify(mealEntryRepository).save(argThat(e ->
                foodId.equals(e.getFoodId()) && new BigDecimal("100").compareTo(e.getQuantityG()) == 0
        ));
    }

    // -----------------------------------------------------------------------
    // createEntry — ad-hoc entry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createEntry persists supplied macros for ad-hoc entry (no foodId)")
    void createEntry_AdHocEntry_PersistsSuppliedMacros() {
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

        final MealEntryDTO result = mealEntryWriteService.createEntry(today, req);

        assertEquals(adHocDTO, result);
        verify(mealEntryRepository).save(argThat(e ->
                e.getFoodId() == null
                        && e.getQuantityG() == null
                        && "Homemade soup".equals(e.getDescription())
                        && new BigDecimal("250.00").compareTo(e.getKcal()) == 0
        ));
        verify(foodRepository, never()).findById(any());
    }

    // -----------------------------------------------------------------------
    // createEntry — error paths
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createEntry throws NoSuchElementException when food is not found")
    void createEntry_FoodNotFound_ThrowsNoSuchElementException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> mealEntryWriteService.createEntry(today, req));

        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEntry throws IllegalArgumentException when food entry has no quantityG")
    void createEntry_FoodEntryMissingQuantityG_ThrowsIllegalArgumentException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, null, null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));

        assertThrows(IllegalArgumentException.class, () -> mealEntryWriteService.createEntry(today, req));

        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEntry throws IllegalArgumentException when ad-hoc entry has no description")
    void createEntry_AdHocMissingDescription_ThrowsIllegalArgumentException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.SNACK, null, null, null,
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00")
        );

        assertThrows(IllegalArgumentException.class, () -> mealEntryWriteService.createEntry(today, req));

        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEntry throws IllegalArgumentException when ad-hoc entry has no macros")
    void createEntry_AdHocMissingMacros_ThrowsIllegalArgumentException() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.SNACK, null, null, "Some food", null, null, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> mealEntryWriteService.createEntry(today, req));

        verify(mealEntryRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // createEntry — day target snapshot persistence
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createEntry delegates day-target snapshot creation to DayTargetSnapshotService")
    void createEntry_DelegatesSnapshotCreationToDayTargetSnapshotService() {
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

        mealEntryWriteService.createEntry(today, req);

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

        mealEntryWriteService.updateEntry(entryId, req);

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

        final MealEntryDTO result = mealEntryWriteService.updateEntry(entryId, req);

        assertEquals(updatedDTO, result);
        verify(mealEntryRepository).save(argThat(e ->
                MealType.DINNER.equals(e.getMealType())
                        && "New soup".equals(e.getDescription())
                        && new BigDecimal("350.00").compareTo(e.getKcal()) == 0
        ));
    }

    @Test
    @DisplayName("updateEntry throws NoSuchElementException when entry not found")
    void updateEntry_NotFound_ThrowsNoSuchElementException() {
        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                MealType.DINNER, null, null, null, null, null, null
        );

        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> mealEntryWriteService.updateEntry(entryId, req));

        verify(mealEntryRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // deleteEntry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteEntry throws NoSuchElementException when entry not found")
    void deleteEntry_NotFound_ThrowsNoSuchElementException() {
        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> mealEntryWriteService.deleteEntry(entryId));

        verify(mealEntryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteEntry deletes when entry exists")
    void deleteEntry_Exists_Deletes() {
        when(mealEntryRepository.findById(entryId)).thenReturn(Optional.of(mealEntryEntity));

        mealEntryWriteService.deleteEntry(entryId);

        verify(mealEntryRepository).delete(mealEntryEntity);
    }

    // -----------------------------------------------------------------------
    // foodName population
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createEntry populates foodName from food catalog for food-backed entry")
    void createEntry_FoodEntry_PopulatesFoodName() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        final MealEntryDTO result = mealEntryWriteService.createEntry(today, req);

        assertEquals("Test Food", result.foodName());
    }

    @Test
    @DisplayName("createEntry leaves foodName null for ad-hoc entry")
    void createEntry_AdHocEntry_FoodNameIsNull() {
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

        final MealEntryDTO result = mealEntryWriteService.createEntry(today, req);

        assertNull(result.foodName());
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

        final MealEntryDTO result = mealEntryWriteService.updateEntry(entryId, req);

        assertEquals("Test Food", result.foodName());
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

        final MealEntryDTO result = mealEntryWriteService.updateEntry(entryId, req);

        assertNull(result.foodName());
    }

    @Test
    @DisplayName("createEntry snapshots foodName onto the saved entity for a food-backed entry")
    void createEntry_FoodEntry_SnapshotsFoodNameOntoSavedEntity() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH, foodId, new BigDecimal("150"), null, null, null, null, null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(foodEntity));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenReturn(mealEntryEntity);
        when(mealEntryMapper.toDTO(mealEntryEntity, "Test Food")).thenReturn(mealEntryDTO);

        mealEntryWriteService.createEntry(today, req);

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

        mealEntryWriteService.updateEntry(entryId, req);

        verify(mealEntryRepository).save(argThat(e -> "Test Food".equals(e.getFoodName())));
    }
}
