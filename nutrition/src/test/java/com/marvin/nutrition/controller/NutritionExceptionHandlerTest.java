package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Unit tests for {@link NutritionExceptionHandler}. */
@DisplayName("NutritionExceptionHandler Tests")
class NutritionExceptionHandlerTest {

    private final NutritionExceptionHandler handler = new NutritionExceptionHandler();

    @Test
    @DisplayName("handleConstraintViolation returns 400")
    void handleConstraintViolation_ReturnsBadRequest() {
        final ConstraintViolationException ex =
                new ConstraintViolationException("size: must be less than or equal to 200", Set.of());

        final ResponseEntity<String> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with weight-specific message for duplicate weight entry date")
    void handleDataIntegrityViolation_WeightEntryConstraint_ReturnsWeightSpecificMessage() {
        final org.hibernate.exception.ConstraintViolationException cause =
                new org.hibernate.exception.ConstraintViolationException(
                        "duplicate key value violates unique constraint", null, "weight_entry_entry_date_key");
        final DataIntegrityViolationException ex = new DataIntegrityViolationException("conflict", cause);

        final ResponseEntity<String> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("A weight entry for this date already exists", response.getBody());
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with generic message for other constraint violations")
    void handleDataIntegrityViolation_OtherConstraint_ReturnsGenericMessage() {
        final org.hibernate.exception.ConstraintViolationException cause =
                new org.hibernate.exception.ConstraintViolationException(
                        "duplicate key value violates unique constraint", null, "day_target_snapshot_pkey");
        final DataIntegrityViolationException ex = new DataIntegrityViolationException("conflict", cause);

        final ResponseEntity<String> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The request conflicts with existing data.", response.getBody());
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with generic message for profile singleton constraint")
    void handleDataIntegrityViolation_ProfileSingletonConstraint_ReturnsGenericMessage() {
        final org.hibernate.exception.ConstraintViolationException cause =
                new org.hibernate.exception.ConstraintViolationException(
                        "check constraint violation", null, "profile_single_row");
        final DataIntegrityViolationException ex = new DataIntegrityViolationException("conflict", cause);

        final ResponseEntity<String> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The request conflicts with existing data.", response.getBody());
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with generic message when cause is not a hibernate constraint violation")
    void handleDataIntegrityViolation_NoRecognizableCause_ReturnsGenericMessage() {
        final DataIntegrityViolationException ex = new DataIntegrityViolationException("some message");

        final ResponseEntity<String> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The request conflicts with existing data.", response.getBody());
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with generic message when constraint name is null")
    void handleDataIntegrityViolation_NullConstraintName_ReturnsGenericMessage() {
        final org.hibernate.exception.ConstraintViolationException cause =
                new org.hibernate.exception.ConstraintViolationException("some violation", null, null);
        final DataIntegrityViolationException ex = new DataIntegrityViolationException("conflict", cause);

        final ResponseEntity<String> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The request conflicts with existing data.", response.getBody());
    }
}
