package com.marvin.nutrition.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marvin.nutrition.entity.ActivityLevel;
import com.marvin.nutrition.entity.Goal;
import com.marvin.nutrition.entity.Sex;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests that validate Bean Validation constraints on {@link ProfileDTO} directly. */
public class ProfileDTOValidationTest {

    private final Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("valid ProfileDTO yields zero violations")
    void profile_valid_noViolations() {
        final ProfileDTO profile = new ProfileDTO(
                null,
                Sex.MALE,
                LocalDate.of(1990, 5, 15),
                new BigDecimal("175"),
                ActivityLevel.MODERATE,
                Goal.MAINTAIN,
                new BigDecimal("2.0"),
                new BigDecimal("0.30"),
                1800
        );

        final Set<ConstraintViolation<ProfileDTO>> violations = validator.validate(profile);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid profile");
    }

    @Test
    @DisplayName("ProfileDTO with null fatPct yields a violation")
    void profile_nullFatPct_yieldsViolation() {
        final ProfileDTO profile = new ProfileDTO(
                null,
                Sex.MALE,
                LocalDate.of(1990, 5, 15),
                new BigDecimal("175"),
                ActivityLevel.MODERATE,
                Goal.MAINTAIN,
                new BigDecimal("2.0"),
                null,
                1800
        );

        final Set<ConstraintViolation<ProfileDTO>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty(), "Expected a violation for null fatPct");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fatPct")),
                "Expected the violation to be on the 'fatPct' property"
        );
    }

    @Test
    @DisplayName("ProfileDTO with fatPct greater than 1 yields a violation")
    void profile_fatPctAboveOne_yieldsViolation() {
        final ProfileDTO profile = new ProfileDTO(
                null,
                Sex.MALE,
                LocalDate.of(1990, 5, 15),
                new BigDecimal("175"),
                ActivityLevel.MODERATE,
                Goal.MAINTAIN,
                new BigDecimal("2.0"),
                new BigDecimal("1.5"),
                1800
        );

        final Set<ConstraintViolation<ProfileDTO>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty(), "Expected a violation for fatPct greater than 1");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fatPct")),
                "Expected the violation to be on the 'fatPct' property"
        );
    }

    @Test
    @DisplayName("ProfileDTO with non-positive fatPct yields a violation")
    void profile_nonPositiveFatPct_yieldsViolation() {
        final ProfileDTO profile = new ProfileDTO(
                null,
                Sex.MALE,
                LocalDate.of(1990, 5, 15),
                new BigDecimal("175"),
                ActivityLevel.MODERATE,
                Goal.MAINTAIN,
                new BigDecimal("2.0"),
                new BigDecimal("0"),
                1800
        );

        final Set<ConstraintViolation<ProfileDTO>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty(), "Expected a violation for non-positive fatPct");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fatPct")),
                "Expected the violation to be on the 'fatPct' property"
        );
    }
}
