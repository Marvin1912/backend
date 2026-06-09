package com.marvin.grocery.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests that validate Bean Validation constraints on receipt item request records directly. */
class ReceiptItemRequestValidationTest {

    private final Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("valid AddReceiptItemRequest yields zero violations")
    void addRequest_valid_noViolations() {
        final AddReceiptItemRequest req = new AddReceiptItemRequest("Vollmilch", 2, new BigDecimal("1.29"));

        final Set<ConstraintViolation<AddReceiptItemRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("AddReceiptItemRequest with null singlePrice yields a violation")
    void addRequest_nullSinglePrice_yieldsViolation() {
        final AddReceiptItemRequest req = new AddReceiptItemRequest("Vollmilch", 2, null);

        final Set<ConstraintViolation<AddReceiptItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null singlePrice");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("singlePrice")),
                "Expected the violation to be on the 'singlePrice' property"
        );
    }

    @Test
    @DisplayName("AddReceiptItemRequest with zero quantity yields a violation")
    void addRequest_zeroQuantity_yieldsViolation() {
        final AddReceiptItemRequest req = new AddReceiptItemRequest("Vollmilch", 0, new BigDecimal("1.29"));

        final Set<ConstraintViolation<AddReceiptItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for zero quantity");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")),
                "Expected the violation to be on the 'quantity' property"
        );
    }

    @Test
    @DisplayName("AddReceiptItemRequest with blank name yields a violation")
    void addRequest_blankName_yieldsViolation() {
        final AddReceiptItemRequest req = new AddReceiptItemRequest("  ", 1, new BigDecimal("1.29"));

        final Set<ConstraintViolation<AddReceiptItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for blank name");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")),
                "Expected the violation to be on the 'name' property"
        );
    }

    @Test
    @DisplayName("valid UpdateReceiptItemRequest yields zero violations")
    void updateRequest_valid_noViolations() {
        final UpdateReceiptItemRequest req = new UpdateReceiptItemRequest("Vollmilch", 2, new BigDecimal("1.29"));

        final Set<ConstraintViolation<UpdateReceiptItemRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateReceiptItemRequest with null singlePrice yields a violation")
    void updateRequest_nullSinglePrice_yieldsViolation() {
        final UpdateReceiptItemRequest req = new UpdateReceiptItemRequest("Vollmilch", 2, null);

        final Set<ConstraintViolation<UpdateReceiptItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for null singlePrice");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("singlePrice")),
                "Expected the violation to be on the 'singlePrice' property"
        );
    }

    @Test
    @DisplayName("UpdateReceiptItemRequest with zero quantity yields a violation")
    void updateRequest_zeroQuantity_yieldsViolation() {
        final UpdateReceiptItemRequest req = new UpdateReceiptItemRequest("Vollmilch", 0, new BigDecimal("1.29"));

        final Set<ConstraintViolation<UpdateReceiptItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for zero quantity");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")),
                "Expected the violation to be on the 'quantity' property"
        );
    }
}
