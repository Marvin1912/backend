package com.marvin.nutrition.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests that validate Bean Validation constraints on {@link MealEstimateRequest} directly. */
public class MealEstimateRequestValidationTest {

    private final Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("valid MealEstimateRequest without portionHint yields zero violations")
    void request_valid_noPortionHint_noViolations() {
        final MealEstimateRequest req = new MealEstimateRequest("Schnitzel mit Pommes", null);

        final Set<ConstraintViolation<MealEstimateRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request without portionHint");
    }

    @Test
    @DisplayName("valid MealEstimateRequest with portionHint yields zero violations")
    void request_valid_withPortionHint_noViolations() {
        final MealEstimateRequest req = new MealEstimateRequest("Spaghetti Bolognese", "one plate");

        final Set<ConstraintViolation<MealEstimateRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request with portionHint");
    }

    @Test
    @DisplayName("blank description yields a violation")
    void request_blankDescription_yieldsViolation() {
        final MealEstimateRequest req = new MealEstimateRequest("   ", null);

        final Set<ConstraintViolation<MealEstimateRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for blank description");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")),
                "Expected the violation to be on the 'description' property"
        );
    }

    @Test
    @DisplayName("null description yields a violation")
    void request_nullDescription_yieldsViolation() {
        final MealEstimateRequest req = new MealEstimateRequest(null, null);

        final Set<ConstraintViolation<MealEstimateRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null description");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")),
                "Expected the violation to be on the 'description' property"
        );
    }

    @Test
    @DisplayName("description exceeding 500 characters yields a violation")
    void request_overLongDescription_yieldsViolation() {
        final String longDescription = "x".repeat(501);
        final MealEstimateRequest req = new MealEstimateRequest(longDescription, null);

        final Set<ConstraintViolation<MealEstimateRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for description exceeding 500 chars");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")),
                "Expected the violation to be on the 'description' property"
        );
    }

    @Test
    @DisplayName("portionHint exceeding 255 characters yields a violation")
    void request_overLongPortionHint_yieldsViolation() {
        final String longHint = "x".repeat(256);
        final MealEstimateRequest req = new MealEstimateRequest("Valid description", longHint);

        final Set<ConstraintViolation<MealEstimateRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for portionHint exceeding 255 chars");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("portionHint")),
                "Expected the violation to be on the 'portionHint' property"
        );
    }
}
