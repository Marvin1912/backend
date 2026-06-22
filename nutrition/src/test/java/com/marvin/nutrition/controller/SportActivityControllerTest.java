package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.CreateSportActivityRequest;
import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.dto.UpdateSportActivityRequest;
import com.marvin.nutrition.entity.SportActivityType;
import com.marvin.nutrition.service.SportActivityService;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/** Unit tests for {@link SportActivityController} covering all endpoints. */
@ExtendWith(MockitoExtension.class)
@DisplayName("SportActivityController Tests")
class SportActivityControllerTest {

    @Mock
    private SportActivityService sportActivityService;

    @InjectMocks
    private SportActivityController sportActivityController;

    private UUID activityId;
    private LocalDate today;
    private SportActivityDTO sportActivityDTO;
    private CreateSportActivityRequest createRequest;
    private UpdateSportActivityRequest updateRequest;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        activityId = UUID.randomUUID();
        today = LocalDate.of(2026, 6, 7);

        sportActivityDTO = new SportActivityDTO(
                activityId, today, SportActivityType.RUNNING, null, new BigDecimal("300.00")
        );

        createRequest = new CreateSportActivityRequest(
                SportActivityType.RUNNING, null, new BigDecimal("300.00")
        );

        updateRequest = new UpdateSportActivityRequest(
                SportActivityType.CYCLING, null, new BigDecimal("400.00")
        );
    }

    // -----------------------------------------------------------------------
    // POST /nutrition/days/{date}/activities
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addActivity returns 201 Created with Location header pointing to new activity")
    void addActivity_Valid_Returns201WithLocation() {
        when(sportActivityService.addActivity(eq(today), any(CreateSportActivityRequest.class)))
                .thenReturn(Mono.just(sportActivityDTO));

        final Mono<ResponseEntity<SportActivityDTO>> result =
                sportActivityController.addActivity(today, createRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(SportActivityType.RUNNING, response.getBody().activityType());
                    assertNotNull(response.getHeaders().getLocation());
                    assertTrue(response.getHeaders().getLocation().toString()
                            .contains(activityId.toString()));
                })
                .verifyComplete();

        verify(sportActivityService).addActivity(eq(today), any(CreateSportActivityRequest.class));
    }

    @Test
    @DisplayName("addActivity returns 400 when service emits IllegalArgumentException")
    void addActivity_OtherTypeMissingDescription_Returns400() {
        when(sportActivityService.addActivity(eq(today), any(CreateSportActivityRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException(
                        "description is required for OTHER activity type")));

        final Mono<ResponseEntity<SportActivityDTO>> result =
                sportActivityController.addActivity(today, createRequest);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(400, response.getStatusCode().value()))
                .verifyComplete();

        verify(sportActivityService).addActivity(eq(today), any(CreateSportActivityRequest.class));
    }

    // -----------------------------------------------------------------------
    // PUT /nutrition/activities/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateActivity returns 200 with updated DTO when activity exists")
    void updateActivity_Found_Returns200WithUpdatedDTO() {
        final SportActivityDTO updatedDTO = new SportActivityDTO(
                activityId, today, SportActivityType.CYCLING, null, new BigDecimal("400.00")
        );

        when(sportActivityService.updateActivity(eq(activityId), any(UpdateSportActivityRequest.class)))
                .thenReturn(Mono.just(updatedDTO));

        final Mono<ResponseEntity<SportActivityDTO>> result =
                sportActivityController.updateActivity(activityId, updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(SportActivityType.CYCLING, response.getBody().activityType());
                })
                .verifyComplete();

        verify(sportActivityService).updateActivity(eq(activityId), any(UpdateSportActivityRequest.class));
    }

    @Test
    @DisplayName("updateActivity returns 404 when activity not found")
    void updateActivity_NotFound_Returns404() {
        when(sportActivityService.updateActivity(eq(activityId), any(UpdateSportActivityRequest.class)))
                .thenReturn(Mono.error(new NoSuchElementException("Sport activity not found: " + activityId)));

        final Mono<ResponseEntity<SportActivityDTO>> result =
                sportActivityController.updateActivity(activityId, updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(sportActivityService).updateActivity(eq(activityId), any(UpdateSportActivityRequest.class));
    }

    @Test
    @DisplayName("updateActivity returns 400 when service emits IllegalArgumentException")
    void updateActivity_OtherTypeMissingDescription_Returns400() {
        when(sportActivityService.updateActivity(eq(activityId), any(UpdateSportActivityRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException(
                        "description is required for OTHER activity type")));

        final Mono<ResponseEntity<SportActivityDTO>> result =
                sportActivityController.updateActivity(activityId, updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(400, response.getStatusCode().value()))
                .verifyComplete();

        verify(sportActivityService).updateActivity(eq(activityId), any(UpdateSportActivityRequest.class));
    }

    // -----------------------------------------------------------------------
    // DELETE /nutrition/activities/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteActivity returns 204 No Content when activity exists")
    void deleteActivity_Exists_Returns204() {
        when(sportActivityService.deleteActivity(activityId)).thenReturn(Mono.empty());

        final Mono<ResponseEntity<Void>> result = sportActivityController.deleteActivity(activityId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();

        verify(sportActivityService).deleteActivity(activityId);
    }

    @Test
    @DisplayName("deleteActivity returns 404 when activity not found")
    void deleteActivity_NotFound_Returns404() {
        when(sportActivityService.deleteActivity(activityId))
                .thenReturn(Mono.error(new NoSuchElementException("Sport activity not found: " + activityId)));

        final Mono<ResponseEntity<Void>> result = sportActivityController.deleteActivity(activityId);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();

        verify(sportActivityService).deleteActivity(activityId);
    }
}
