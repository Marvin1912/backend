package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
