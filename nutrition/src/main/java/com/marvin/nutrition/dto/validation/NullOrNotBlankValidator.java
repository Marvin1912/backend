package com.marvin.nutrition.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that a {@link String} value is either {@code null} or non-blank.
 * Blank or whitespace-only strings are rejected, while {@code null} is accepted so that
 * partial-update requests may omit the field.
 */
public class NullOrNotBlankValidator implements ConstraintValidator<NullOrNotBlank, String> {

    /**
     * Initializes the validator. No setup is required.
     *
     * @param constraintAnnotation the annotation instance for this constraint
     */
    @Override
    public void initialize(final NullOrNotBlank constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    /**
     * Checks that the value is either {@code null} or a non-blank string.
     *
     * @param value   the value to validate
     * @param context the constraint validator context
     * @return {@code true} if the value is {@code null} or non-blank
     */
    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        return value == null || !value.isBlank();
    }
}
