package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealPlanRowRequest;
import com.marvin.nutrition.dto.MealPlanHeaderDTO;
import com.marvin.nutrition.dto.MealPlanRowDTO;
import com.marvin.nutrition.dto.MealPlanSectionDTO;
import com.marvin.nutrition.dto.MealPlanSourceDTO;
import com.marvin.nutrition.dto.UpdateMealPlanRequest;
import com.marvin.nutrition.dto.UpdateMealPlanRowRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSectionRequest;
import com.marvin.nutrition.dto.UpdateMealPlanSourceRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link MealPlanWriteService}, covering the header/source updates it owns
 * directly, and the delegation to {@link MealPlanSectionWriteService} for section/row operations
 * (issue #225 rewrite).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanWriteService Tests")
class MealPlanWriteServiceTest {

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private MealPlanSourceRepository mealPlanSourceRepository;

    @Mock
    private MealPlanMapper mealPlanMapper;

    @Mock
    private MealPlanSectionWriteService mealPlanSectionWriteService;

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
        entity.setFooterNote("old footer");

        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("new eyebrow", null, null, null);

        when(mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)).thenReturn(Optional.of(entity));
        when(mealPlanRepository.save(entity)).thenReturn(entity);

        final MealPlanHeaderDTO result = mealPlanWriteService.updateMealPlan(req);

        assertEquals("new eyebrow", entity.getEyebrow());
        assertEquals("old title", entity.getTitle());
        assertEquals(new MealPlanHeaderDTO("new eyebrow", "old title", "old description", "old footer"), result);
    }

    @Test
    @DisplayName("updateMealPlan throws NoSuchElementException when the singleton row is missing")
    void updateMealPlan_NotFound_ThrowsNoSuchElementException() {
        when(mealPlanRepository.findById(MealPlanEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("eyebrow", null, null, null);

        assertThrows(NoSuchElementException.class, () -> mealPlanWriteService.updateMealPlan(req));
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
    // delegation to MealPlanSectionWriteService
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateSection delegates to MealPlanSectionWriteService")
    void updateSection_DelegatesToSectionWriteService() {
        final UUID sectionId = UUID.randomUUID();
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("title", null, null);
        final MealPlanSectionDTO sectionDTO = new MealPlanSectionDTO(sectionId, "title", "note", List.of(), null);

        when(mealPlanSectionWriteService.updateSection(sectionId, req)).thenReturn(sectionDTO);

        final MealPlanSectionDTO result = mealPlanWriteService.updateSection(sectionId, req);

        assertEquals(sectionDTO, result);
    }

    @Test
    @DisplayName("addRow delegates to MealPlanSectionWriteService")
    void addRow_DelegatesToSectionWriteService() {
        final UUID sectionId = UUID.randomUUID();
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(
                UUID.randomUUID(), MealType.BREAKFAST, UUID.randomUUID(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));

        when(mealPlanSectionWriteService.addRow(sectionId, req)).thenReturn(rowDTO);

        final MealPlanRowDTO result = mealPlanWriteService.addRow(sectionId, req);

        assertEquals(rowDTO, result);
    }

    @Test
    @DisplayName("addRows delegates to MealPlanSectionWriteService")
    void addRows_DelegatesToSectionWriteService() {
        final UUID sectionId = UUID.randomUUID();
        final List<CreateMealPlanRowRequest> requests = List.of(
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00")));
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(
                UUID.randomUUID(), MealType.BREAKFAST, UUID.randomUUID(), "Haferflocken",
                new BigDecimal("90.00"), new BigDecimal("519.00"), new BigDecimal("28.00"),
                new BigDecimal("60.00"), new BigDecimal("10.00"));

        when(mealPlanSectionWriteService.addRows(sectionId, requests)).thenReturn(List.of(rowDTO));

        final List<MealPlanRowDTO> result = mealPlanWriteService.addRows(sectionId, requests);

        assertEquals(List.of(rowDTO), result);
    }

    @Test
    @DisplayName("updateRow delegates to MealPlanSectionWriteService")
    void updateRow_DelegatesToSectionWriteService() {
        final UUID rowId = UUID.randomUUID();
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, UUID.randomUUID(), new BigDecimal("100.00"));
        final MealPlanRowDTO rowDTO = new MealPlanRowDTO(
                rowId, MealType.LUNCH, UUID.randomUUID(), "Haferflocken",
                new BigDecimal("100.00"), new BigDecimal("577.00"), new BigDecimal("31.00"),
                new BigDecimal("66.00"), new BigDecimal("11.00"));

        when(mealPlanSectionWriteService.updateRow(rowId, req)).thenReturn(rowDTO);

        final MealPlanRowDTO result = mealPlanWriteService.updateRow(rowId, req);

        assertEquals(rowDTO, result);
    }

    @Test
    @DisplayName("deleteRow delegates to MealPlanSectionWriteService")
    void deleteRow_DelegatesToSectionWriteService() {
        final UUID rowId = UUID.randomUUID();

        mealPlanWriteService.deleteRow(rowId);

        verify(mealPlanSectionWriteService).deleteRow(rowId);
    }
}
