package com.marvin.nutrition.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marvin.nutrition.entity.MealType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests that validate Bean Validation constraints on meal entry request records directly. */
public class MealEntryRequestValidationTest {

    private final Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("valid CreateMealEntryRequest (food-backed) yields zero violations")
    void createRequest_valid_foodBacked_noViolations() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH,
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                null,
                null,
                null,
                null,
                null
        );

        final Set<ConstraintViolation<CreateMealEntryRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid food-backed request");
    }

    @Test
    @DisplayName("CreateMealEntryRequest with negative quantityG yields a violation")
    void createRequest_negativeQuantityG_yieldsViolation() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.LUNCH,
                UUID.randomUUID(),
                new BigDecimal("-1.00"),
                null,
                null,
                null,
                null,
                null
        );

        final Set<ConstraintViolation<CreateMealEntryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for negative quantityG");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityG")),
                "Expected the violation to be on the 'quantityG' property"
        );
    }

    @Test
    @DisplayName("CreateMealEntryRequest with negative kcal yields a violation")
    void createRequest_negativeKcal_yieldsViolation() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                MealType.DINNER,
                null,
                null,
                "Homemade soup",
                new BigDecimal("-100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("5.00")
        );

        final Set<ConstraintViolation<CreateMealEntryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for negative kcal");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("kcal")),
                "Expected the violation to be on the 'kcal' property"
        );
    }

    @Test
    @DisplayName("CreateMealEntryRequest with null mealType yields a violation")
    void createRequest_nullMealType_yieldsViolation() {
        final CreateMealEntryRequest req = new CreateMealEntryRequest(
                null,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                null,
                null,
                null,
                null
        );

        final Set<ConstraintViolation<CreateMealEntryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null mealType");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("mealType")),
                "Expected the violation to be on the 'mealType' property"
        );
    }

    @Test
    @DisplayName("UpdateMealEntryRequest with negative quantityG yields a violation")
    void updateRequest_negativeQuantityG_yieldsViolation() {
        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                null,
                new BigDecimal("-50.00"),
                null,
                null,
                null,
                null,
                null
        );

        final Set<ConstraintViolation<UpdateMealEntryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for negative quantityG");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityG")),
                "Expected the violation to be on the 'quantityG' property"
        );
    }

    @Test
    @DisplayName("UpdateMealEntryRequest with over-long description yields a violation")
    void updateRequest_overLongDescription_yieldsViolation() {
        final String longDescription = "x".repeat(256);
        final UpdateMealEntryRequest req = new UpdateMealEntryRequest(
                null,
                null,
                longDescription,
                null,
                null,
                null,
                null
        );

        final Set<ConstraintViolation<UpdateMealEntryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for description exceeding 255 chars");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")),
                "Expected the violation to be on the 'description' property"
        );
    }
}
