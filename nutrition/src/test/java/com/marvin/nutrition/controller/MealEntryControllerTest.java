package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateMealEntriesRequest;
import com.marvin.nutrition.dto.CreateMealEntryRequest;
import com.marvin.nutrition.dto.DaySummaryDTO;
import com.marvin.nutrition.dto.MacrosDTO;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.dto.UpdateMealEntryRequest;
import com.marvin.nutrition.entity.MealType;
import com.marvin.nutrition.service.MealEntryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link MealEntryController} covering all endpoints. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealEntryController Tests")
class MealEntryControllerTest {

    @Mock
    private MealEntryService mealEntryService;

    @InjectMocks
    private MealEntryController mealEntryController;

    private UUID entryId;
    private LocalDate today;
    private MealEntryDTO mealEntryDTO;
    private CreateMealEntryRequest createRequest;
    private UpdateMealEntryRequest updateRequest;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        entryId = UUID.randomUUID();
        today = LocalDate.of(2026, 6, 7);

        mealEntryDTO = new MealEntryDTO(
                entryId, today, MealType.LUNCH, null, "Salad", null,
                new BigDecimal("300.00"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00"), null
        );

        createRequest = new CreateMealEntryRequest(
                MealType.LUNCH, null, null, "Salad",
                new BigDecimal("300.00"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00")
        );

        updateRequest = new UpdateMealEntryRequest(
                MealType.DINNER, null, "Updated salad", null, null, null, null
        );
    }

    // -----------------------------------------------------------------------
    // POST /nutrition/days/{date}/entries
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntry returns 201 Created with Location header pointing to new entry")
    void addEntry_Valid_Returns201WithLocation() {
        when(mealEntryService.addEntry(eq(today), any(CreateMealEntryRequest.class)))
                .thenReturn(Mono.just(mealEntryDTO));

        final Mono<ResponseEntity<MealEntryDTO>> result =
                mealEntryController.addEntry(today, createRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(MealType.LUNCH, response.getBody().mealType());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(response.getHeaders().getLocation().toString()
                            .contains(entryId.toString()));
                })
                .verifyComplete();

        verify(mealEntryService).addEntry(eq(today), any(CreateMealEntryRequest.class));
    }

    // -----------------------------------------------------------------------
    // POST /nutrition/days/{date}/entries/batch
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addEntries returns 201 Created with list body")
    void addEntries_Valid_Returns201WithListBody() {
        final MealEntryDTO second = new MealEntryDTO(
                UUID.randomUUID(), today, MealType.DINNER, null, "Soup", null,
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("8.00"), null
        );
        final List<CreateMealEntryRequest> requests = List.of(createRequest, createRequest);
        final CreateMealEntriesRequest batchRequest = new CreateMealEntriesRequest(requests);

        when(mealEntryService.addEntries(eq(today), eq(requests)))
                .thenReturn(Mono.just(List.of(mealEntryDTO, second)));

        final Mono<ResponseEntity<List<MealEntryDTO>>> result =
                mealEntryController.addEntries(today, batchRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(2, response.getBody().size());
                })
                .verifyComplete();

        verify(mealEntryService).addEntries(eq(today), eq(requests));
    }

    @Test
    @DisplayName("addEntries returns 404 when service emits NoSuchElementException")
    void addEntries_UnknownFoodId_Returns404() {
        final List<CreateMealEntryRequest> requests = List.of(createRequest);
        final CreateMealEntriesRequest batchRequest = new CreateMealEntriesRequest(requests);

        when(mealEntryService.addEntries(eq(today), eq(requests)))
                .thenReturn(Mono.error(new NoSuchElementException("Food not found")));

        final Mono<ResponseEntity<List<MealEntryDTO>>> result =
                mealEntryController.addEntries(today, batchRequest);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealEntryService).addEntries(eq(today), eq(requests));
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/days/{date}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getDay returns 200 with DaySummaryDTO")
    void getDay_ReturnsDay_Returns200WithSummary() {
        final MacrosDTO totals = new MacrosDTO(
                new BigDecimal("300.00"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00")
        );
        final TargetsDTO targets = new TargetsDTO(1700, 2200, 2000, 150, 67, 248, "MIFFLIN_ST_JEOR");
        final MacrosDTO remaining = new MacrosDTO(
                new BigDecimal("1700.00"), new BigDecimal("130.00"),
                new BigDecimal("208.00"), new BigDecimal("57.00")
        );
        final DaySummaryDTO summaryDTO = new DaySummaryDTO(
                today, List.of(mealEntryDTO), totals, targets, remaining
        );

        when(mealEntryService.getDay(today)).thenReturn(Mono.just(summaryDTO));

        final Mono<DaySummaryDTO> result = mealEntryController.getDay(today);

        StepVerifier.create(result)
                .assertNext(summary -> {
                    assertEquals(today, summary.date());
                    assertEquals(1, summary.entries().size());
                    assertNotNull(summary.totals());
                    assertNotNull(summary.targets());
                    assertNotNull(summary.remaining());
                })
                .verifyComplete();

        verify(mealEntryService).getDay(today);
    }

    // -----------------------------------------------------------------------
    // GET /nutrition/days
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getDays returns 200 with one DaySummaryDTO per day in range")
    void getDays_ReturnsDays_Returns200WithSummaries() {
        final LocalDate from = today.minusDays(1);
        final MacrosDTO totals = new MacrosDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        final DaySummaryDTO day1 = new DaySummaryDTO(from, List.of(), totals, null, null);
        final DaySummaryDTO day2 = new DaySummaryDTO(today, List.of(mealEntryDTO), totals, null, null);

        when(mealEntryService.getDays(from, today)).thenReturn(Mono.just(List.of(day1, day2)));

        final Mono<List<DaySummaryDTO>> result = mealEntryController.getDays(from, today);

        StepVerifier.create(result)
                .assertNext(summaries -> {
                    assertEquals(2, summaries.size());
                    assertEquals(from, summaries.get(0).date());
                    assertEquals(today, summaries.get(1).date());
                })
                .verifyComplete();

        verify(mealEntryService).getDays(from, today);
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/entries/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateEntry returns 200 with updated DTO when entry exists")
    void updateEntry_Found_Returns200WithUpdatedDTO() {
        final MealEntryDTO updatedDTO = new MealEntryDTO(
                entryId, today, MealType.DINNER, null, "Updated salad", null,
                new BigDecimal("300.00"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00"), null
        );

        when(mealEntryService.updateEntry(eq(entryId), any(UpdateMealEntryRequest.class)))
                .thenReturn(Mono.just(updatedDTO));

        final Mono<ResponseEntity<MealEntryDTO>> result =
                mealEntryController.updateEntry(entryId, updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(MealType.DINNER, response.getBody().mealType());
                })
                .verifyComplete();

        verify(mealEntryService).updateEntry(eq(entryId), any(UpdateMealEntryRequest.class));
    }

    @Test
    @DisplayName("updateEntry returns 404 when entry not found")
    void updateEntry_NotFound_Returns404() {
        when(mealEntryService.updateEntry(eq(entryId), any(UpdateMealEntryRequest.class)))
                .thenReturn(Mono.error(new NoSuchElementException("Meal entry not found: " + entryId)));

        final Mono<ResponseEntity<MealEntryDTO>> result =
                mealEntryController.updateEntry(entryId, updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealEntryService).updateEntry(eq(entryId), any(UpdateMealEntryRequest.class));
    }

    // -----------------------------------------------------------------------
    // DELETE /nutrition/entries/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteEntry returns 204 No Content when entry exists")
    void deleteEntry_Exists_Returns204() {
        when(mealEntryService.deleteEntry(entryId)).thenReturn(Mono.empty());

        final Mono<ResponseEntity<Void>> result = mealEntryController.deleteEntry(entryId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealEntryService).deleteEntry(entryId);
    }

    @Test
    @DisplayName("deleteEntry returns 404 when entry not found")
    void deleteEntry_NotFound_Returns404() {
        when(mealEntryService.deleteEntry(entryId))
                .thenReturn(Mono.error(new NoSuchElementException("Meal entry not found: " + entryId)));

        final Mono<ResponseEntity<Void>> result = mealEntryController.deleteEntry(entryId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(mealEntryService).deleteEntry(entryId);
    }
}
