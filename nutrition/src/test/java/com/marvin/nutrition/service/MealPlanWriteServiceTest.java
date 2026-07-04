package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealPlanChangelogEntryRequest;
import com.marvin.nutrition.dto.MealPlanChangelogEntryDTO;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanShoppingCategoryDTO;
import com.marvin.nutrition.dto.MealPlanShoppingItemDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.MealPlanStatDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingCategoryRequest;
import com.marvin.nutrition.dto.UpdateMealPlanShoppingItemRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
import com.marvin.nutrition.dto.UpdateMealPlanStatRequest;
import com.marvin.nutrition.entity.MealPlanChangelogEntryEntity;
import com.marvin.nutrition.entity.MealPlanEntity;
import com.marvin.nutrition.entity.MealPlanSourceEntity;
import com.marvin.nutrition.entity.MealPlanStatEntity;
import com.marvin.nutrition.mapper.MealPlanMapper;
import com.marvin.nutrition.repository.MealPlanChangelogEntryRepository;
import com.marvin.nutrition.repository.MealPlanRepository;
import com.marvin.nutrition.repository.MealPlanSourceRepository;
import com.marvin.nutrition.repository.MealPlanStatRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link MealPlanWriteService}, covering the header/stat/source/changelog updates it
 * owns directly, and the delegation to {@link MealPlanSectionWriteService} and
 * {@link MealPlanShoppingListWriteService} for section/row and shopping category/item updates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanWriteService Tests")
class MealPlanWriteServiceTest {

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private MealPlanStatRepository mealPlanStatRepository;

    @Mock
    private MealPlanSourceRepository mealPlanSourceRepository;

    @Mock
    private MealPlanChangelogEntryRepository mealPlanChangelogEntryRepository;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @Mock
    private MealPlanSectionWriteService mealPlanSectionWriteService;

    @Mock
    private MealPlanShoppingListWriteService mealPlanShoppingListWriteService;

    @InjectMocks
    private MealPlanWriteService mealPlanWriteService;

    // -----------------------------------------------------------------------
    // updateMealPlan
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateMealPlan applies non-null fields, saves and returns the updated header")
    void updateMealPlan_AppliesNonNullFields_ReturnsUpdatedHeader() {
        final MealPlanEntity entity = new MealPlanEntity();
        entity.setId(MealPlanEntity.SINGLETON_ID);
        entity.setEyebrow("old eyebrow");
        entity.setTitle("old title");
        entity.setDescription("old description");
        entity.setShoppingListTitle("old shopping title");
        entity.setShoppingListNote("old shopping note");
        entity.setFooterNote("old footer");

        final UpdateMealPlanRequest req =
                new UpdateMealPlanRequest("new eyebrow", null, null, null, null, null, null);

        when(mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)).thenReturn(Optional.of(entity));
        when(mealPlanRepository.save(entity)).thenReturn(entity);

        final MealPlanHeaderDTO result = mealPlanWriteService.updateMealPlan(req);

        assertEquals("new eyebrow", entity.getEyebrow());
        assertEquals("old title", entity.getTitle());
        assertEquals(new MealPlanHeaderDTO(
                "new eyebrow", "old title", "old description",
                "old shopping title", "old shopping note", null, "old footer"), result);
    }

    @Test
    @DisplayName("updateMealPlan throws NoSuchElementException when the singleton row is missing")
    void updateMealPlan_NotFound_ThrowsNoSuchElementException() {
        when(mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        final UpdateMealPlanRequest req =
                new UpdateMealPlanRequest("eyebrow", null, null, null, null, null, null);

        assertThrows(NoSuchElementException.class, () -> mealPlanWriteService.updateMealPlan(req));
    }

    // -----------------------------------------------------------------------
    // updateStat
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateStat applies non-null fields, saves and returns the updated stat")
    void updateStat_AppliesNonNullFields_ReturnsUpdatedStat() {
        final UUID statId = UUID.randomUUID();
        final MealPlanStatEntity stat = new MealPlanStatEntity();
        stat.setId(statId);
        stat.setLabel("old label");
        stat.setValue("old value");

        final MealPlanStatDTO statDTO = new MealPlanStatDTO(statId, "new label", "old value");
        final UpdateMealPlanStatRequest req = new UpdateMealPlanStatRequest("new label", null);

        when(mealPlanStatRepository.findById(statId)).thenReturn(Optional.of(stat));
        when(mealPlanStatRepository.save(stat)).thenReturn(stat);
        when(mealPlanMapper.toStatDTO(stat)).thenReturn(statDTO);

        final MealPlanStatDTO result = mealPlanWriteService.updateStat(statId, req);

        assertEquals("new label", stat.getLabel());
        assertEquals(statDTO, result);
    }

    @Test
    @DisplayName("updateStat throws NoSuchElementException when the stat does not exist")
    void updateStat_NotFound_ThrowsNoSuchElementException() {
        final UUID statId = UUID.randomUUID();
        when(mealPlanStatRepository.findById(statId)).thenReturn(Optional.empty());

        final UpdateMealPlanStatRequest req = new UpdateMealPlanStatRequest("label", null);

        assertThrows(NoSuchElementException.class, () -> mealPlanWriteService.updateStat(statId, req));
    }

    // -----------------------------------------------------------------------
    // updateSource
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateSource applies non-null fields, saves and returns the updated source")
    void updateSource_AppliesNonNullFields_ReturnsUpdatedSource() {
        final UUID sourceId = UUID.randomUUID();
        final MealPlanSourceEntity source = new MealPlanSourceEntity();
        source.setId(sourceId);
        source.setLabel("old label");
        source.setUrl("https://old.example.com");

        final MealPlanSourceDTO sourceDTO = new MealPlanSourceDTO(sourceId, "new label", "https://old.example.com");
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("new label", null);

        when(mealPlanSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(mealPlanSourceRepository.save(source)).thenReturn(source);
        when(mealPlanMapper.toSourceDTO(source)).thenReturn(sourceDTO);

        final MealPlanSourceDTO result = mealPlanWriteService.updateSource(sourceId, req);

        assertEquals("new label", source.getLabel());
        assertEquals(sourceDTO, result);
    }

    @Test
    @DisplayName("updateSource throws NoSuchElementException when the source does not exist")
    void updateSource_NotFound_ThrowsNoSuchElementException() {
        final UUID sourceId = UUID.randomUUID();
        when(mealPlanSourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("label", null);

        assertThrows(NoSuchElementException.class, () -> mealPlanWriteService.updateSource(sourceId, req));
    }

    // -----------------------------------------------------------------------
    // deleteSource
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteSource deletes the found source entity")
    void deleteSource_DeletesFoundEntity() {
        final UUID sourceId = UUID.randomUUID();
        final MealPlanSourceEntity source = new MealPlanSourceEntity();
        source.setId(sourceId);

        when(mealPlanSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));

        mealPlanWriteService.deleteSource(sourceId);

        verify(mealPlanSourceRepository).delete(source);
    }

    @Test
    @DisplayName("deleteSource throws NoSuchElementException when the source does not exist")
    void deleteSource_NotFound_ThrowsNoSuchElementException() {
        final UUID sourceId = UUID.randomUUID();
        when(mealPlanSourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> mealPlanWriteService.deleteSource(sourceId));
    }

    // -----------------------------------------------------------------------
    // addChangelogEntry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addChangelogEntry creates, saves and returns the new changelog entry")
    void addChangelogEntry_CreatesAndReturnsEntry() {
        final CreateMealPlanChangelogEntryRequest req =
                new CreateMealPlanChangelogEntryRequest("Whey", "80 g/Tag", "-> 40 g/Tag", 0);

        final MealPlanChangelogEntryEntity saved = new MealPlanChangelogEntryEntity();
        saved.setId(UUID.randomUUID());
        final MealPlanChangelogEntryDTO dto =
                new MealPlanChangelogEntryDTO(saved.getId(), "Whey", "80 g/Tag", "-> 40 g/Tag");

        when(mealPlanChangelogEntryRepository.save(any(MealPlanChangelogEntryEntity.class))).thenReturn(saved);
        when(mealPlanMapper.toChangelogEntryDTO(saved)).thenReturn(dto);

        final MealPlanChangelogEntryDTO result = mealPlanWriteService.addChangelogEntry(req);

        assertEquals(dto, result);
        verify(mealPlanChangelogEntryRepository).save(any(MealPlanChangelogEntryEntity.class));
    }

    // -----------------------------------------------------------------------
    // delegation to MealPlanSectionWriteService / MealPlanShoppingListWriteService
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateSection delegates to MealPlanSectionWriteService")
    void updateSection_DelegatesToSectionWriteService() {
        final UUID sectionId = UUID.randomUUID();
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("title", null, null);
        final MealPlanSectionDTO sectionDTO = new MealPlanSectionDTO(sectionId, "title", "note", java.util.List.of(), null, null);

        when(mealPlanSectionWriteService.updateSection(sectionId, req)).thenReturn(sectionDTO);

        final MealPlanSectionDTO result = mealPlanWriteService.updateSection(sectionId, req);

        assertEquals(sectionDTO, result);
    }

    @Test
    @DisplayName("updateRow delegates to MealPlanSectionWriteService")
    void updateRow_DelegatesToSectionWriteService() {
        final UUID rowId = UUID.randomUUID();
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest("meal", null, null, null, null);
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(rowId, "meal", "details", "qty", "kcal", "protein");

        when(mealPlanSectionWriteService.updateRow(rowId, req)).thenReturn(rowDTO);

        final MealPlanRowDTO result = mealPlanWriteService.updateRow(rowId, req);

        assertEquals(rowDTO, result);
    }

    @Test
    @DisplayName("updateShoppingCategory delegates to MealPlanShoppingListWriteService")
    void updateShoppingCategory_DelegatesToShoppingListWriteService() {
        final UUID categoryId = UUID.randomUUID();
        final UpdateMealPlanShoppingCategoryRequest req = new UpdateMealPlanShoppingCategoryRequest("title");
        final MealPlanShoppingCategoryDTO categoryDTO =
                new MealPlanShoppingCategoryDTO(categoryId, "title", java.util.List.of());

        when(mealPlanShoppingListWriteService.updateShoppingCategory(categoryId, req)).thenReturn(categoryDTO);

        final MealPlanShoppingCategoryDTO result = mealPlanWriteService.updateShoppingCategory(categoryId, req);

        assertEquals(categoryDTO, result);
    }

    @Test
    @DisplayName("updateShoppingItem delegates to MealPlanShoppingListWriteService")
    void updateShoppingItem_DelegatesToShoppingListWriteService() {
        final UUID itemId = UUID.randomUUID();
        final UpdateMealPlanShoppingItemRequest req = new UpdateMealPlanShoppingItemRequest("name", null, null, null, null);
        final MealPlanShoppingItemDTO itemDTO = new MealPlanShoppingItemDTO(itemId, "name", null, null, null, "qty");

        when(mealPlanShoppingListWriteService.updateShoppingItem(itemId, req)).thenReturn(itemDTO);

        final MealPlanShoppingItemDTO result = mealPlanWriteService.updateShoppingItem(itemId, req);

        assertEquals(itemDTO, result);
    }
}
