package com.marvin.nutrition.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marvin.nutrition.entity.MealType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests that validate Bean Validation constraints on the meal-plan write request records,
 * ensuring oversized/malformed input is rejected as a 400 instead of reaching the database and
 * surfacing as a generic 409 {@code DataIntegrityViolationException}.
 */
public class MealPlanRequestValidationTest {

    private final Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("UpdateMealPlanRequest with all valid-length fields yields zero violations")
    void updateMealPlanRequest_valid_noViolations() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("eyebrow", "title", "description", "footerNote");

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with over-long eyebrow yields a violation on 'eyebrow'")
    void updateMealPlanRequest_overLongEyebrow_yieldsViolation() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest("x".repeat(501), null, null, null);

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for eyebrow exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("eyebrow")));
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with over-long title yields a violation on 'title'")
    void updateMealPlanRequest_overLongTitle_yieldsViolation() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(null, "x".repeat(501), null, null);

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for title exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with unbounded TEXT fields at large size yields zero violations")
    void updateMealPlanRequest_largeUnboundedTextFields_noViolations() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(null, null, "x".repeat(5000), "x".repeat(5000));

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for unbounded TEXT columns regardless of length");
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with valid-length fields yields zero violations")
    void updateMealPlanSectionRequest_valid_noViolations() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("title", "note", "callout");

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with over-long title yields a violation")
    void updateMealPlanSectionRequest_overLongTitle_yieldsViolation() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("x".repeat(501), null, null);

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for title exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with over-long note yields a violation")
    void updateMealPlanSectionRequest_overLongNote_yieldsViolation() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest(null, "x".repeat(501), null);

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for note exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("note")));
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with large unbounded callout yields zero violations")
    void updateMealPlanSectionRequest_largeCallout_noViolations() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest(null, null, "x".repeat(5000));

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for unbounded TEXT column regardless of length");
    }

    @Test
    @DisplayName("UpdateMealPlanSourceRequest with valid-length fields yields zero violations")
    void updateMealPlanSourceRequest_valid_noViolations() {
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("label", "https://example.com");

        final Set<ConstraintViolation<UpdateMealPlanSourceRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanSourceRequest with over-long label yields a violation")
    void updateMealPlanSourceRequest_overLongLabel_yieldsViolation() {
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("x".repeat(501), null);

        final Set<ConstraintViolation<UpdateMealPlanSourceRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for label exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("label")));
    }

    @Test
    @DisplayName("UpdateMealPlanSourceRequest with over-long url yields a violation")
    void updateMealPlanSourceRequest_overLongUrl_yieldsViolation() {
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest(null, "x".repeat(1001));

        final Set<ConstraintViolation<UpdateMealPlanSourceRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for url exceeding 1000 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("url")));
    }

    // -----------------------------------------------------------------------
    // CreateMealPlanRowRequest
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("CreateMealPlanRowRequest with valid fields yields zero violations")
    void createMealPlanRowRequest_valid_noViolations() {
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));

        final Set<ConstraintViolation<CreateMealPlanRowRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("CreateMealPlanRowRequest with null mealType yields a violation")
    void createMealPlanRowRequest_nullMealType_yieldsViolation() {
        final CreateMealPlanRowRequest req = new CreateMealPlanRowRequest(null, UUID.randomUUID(), new BigDecimal("90.00"));

        final Set<ConstraintViolation<CreateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null mealType");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("mealType")));
    }

    @Test
    @DisplayName("CreateMealPlanRowRequest with null foodId yields a violation")
    void createMealPlanRowRequest_nullFoodId_yieldsViolation() {
        final CreateMealPlanRowRequest req = new CreateMealPlanRowRequest(MealType.BREAKFAST, null, new BigDecimal("90.00"));

        final Set<ConstraintViolation<CreateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null foodId");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("foodId")));
    }

    @Test
    @DisplayName("CreateMealPlanRowRequest with null quantityG yields a violation")
    void createMealPlanRowRequest_nullQuantityG_yieldsViolation() {
        final CreateMealPlanRowRequest req = new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), null);

        final Set<ConstraintViolation<CreateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null quantityG");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityG")));
    }

    @Test
    @DisplayName("CreateMealPlanRowRequest with zero quantityG yields a violation")
    void createMealPlanRowRequest_zeroQuantityG_yieldsViolation() {
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), BigDecimal.ZERO);

        final Set<ConstraintViolation<CreateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for zero quantityG");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityG")));
    }

    @Test
    @DisplayName("CreateMealPlanRowRequest with negative quantityG yields a violation")
    void createMealPlanRowRequest_negativeQuantityG_yieldsViolation() {
        final CreateMealPlanRowRequest req =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("-10.00"));

        final Set<ConstraintViolation<CreateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for negative quantityG");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityG")));
    }

    // -----------------------------------------------------------------------
    // CreateMealPlanRowsRequest
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("CreateMealPlanRowsRequest with a non-empty list yields zero violations")
    void createMealPlanRowsRequest_valid_noViolations() {
        final CreateMealPlanRowsRequest req = new CreateMealPlanRowsRequest(
                List.of(new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"))));

        final Set<ConstraintViolation<CreateMealPlanRowsRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("CreateMealPlanRowsRequest with an empty list yields a violation")
    void createMealPlanRowsRequest_emptyList_yieldsViolation() {
        final CreateMealPlanRowsRequest req = new CreateMealPlanRowsRequest(List.of());

        final Set<ConstraintViolation<CreateMealPlanRowsRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for an empty rows list");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rows")));
    }

    @Test
    @DisplayName("CreateMealPlanRowsRequest with more than 50 rows yields a violation")
    void createMealPlanRowsRequest_tooManyRows_yieldsViolation() {
        final CreateMealPlanRowRequest row =
                new CreateMealPlanRowRequest(MealType.BREAKFAST, UUID.randomUUID(), new BigDecimal("90.00"));
        final CreateMealPlanRowsRequest req = new CreateMealPlanRowsRequest(List.of(
                row, row, row, row, row, row, row, row, row, row,
                row, row, row, row, row, row, row, row, row, row,
                row, row, row, row, row, row, row, row, row, row,
                row, row, row, row, row, row, row, row, row, row,
                row, row, row, row, row, row, row, row, row, row, row));

        final Set<ConstraintViolation<CreateMealPlanRowsRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for more than 50 rows");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rows")));
    }

    @Test
    @DisplayName("CreateMealPlanRowsRequest with an invalid nested row yields a violation on the nested path")
    void createMealPlanRowsRequest_invalidNestedRow_yieldsViolation() {
        final CreateMealPlanRowsRequest req = new CreateMealPlanRowsRequest(
                List.of(new CreateMealPlanRowRequest(null, UUID.randomUUID(), new BigDecimal("90.00"))));

        final Set<ConstraintViolation<CreateMealPlanRowsRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation cascading from the nested invalid row");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("mealType")));
    }

    // -----------------------------------------------------------------------
    // UpdateMealPlanRowRequest
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("UpdateMealPlanRowRequest with a null mealType (partial update) yields zero violations")
    void updateMealPlanRowRequest_nullMealType_noViolations() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(null, UUID.randomUUID(), new BigDecimal("90.00"));

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations when mealType is omitted");
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with null foodId yields a violation")
    void updateMealPlanRowRequest_nullFoodId_yieldsViolation() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, null, new BigDecimal("90.00"));

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null foodId");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("foodId")));
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with null quantityG yields a violation")
    void updateMealPlanRowRequest_nullQuantityG_yieldsViolation() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, UUID.randomUUID(), null);

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null quantityG");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityG")));
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with zero or negative quantityG yields a violation")
    void updateMealPlanRowRequest_nonPositiveQuantityG_yieldsViolation() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(MealType.LUNCH, UUID.randomUUID(), BigDecimal.ZERO);

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for non-positive quantityG");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityG")));
    }
}
