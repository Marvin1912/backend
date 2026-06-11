package com.marvin.nutrition.dto.validation;

import com.marvin.nutrition.dto.CreateMealEntryRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that ad-hoc {@link CreateMealEntryRequest} instances (i.e. {@code foodId == null})
 * carry a non-blank {@code description}. Food-backed entries are exempt since their description
 * remains optional. The violation is reported on the {@code description} property so it surfaces
 * as a field error.
 */
public class AdHocDescriptionRequiredValidator implements ConstraintValidator<AdHocDescriptionRequired, CreateMealEntryRequest> {

    /**
     * Initializes the validator. No setup is required.
     *
     * @param constraintAnnotation the annotation instance for this constraint
     */
    @Override
    public void initialize(final AdHocDescriptionRequired constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    /**
     * Checks that ad-hoc entries (where {@code foodId} is {@code null}) have a non-blank description.
     * Food-backed entries and {@code null} requests are always considered valid by this constraint.
     *
     * @param value   the request to validate
     * @param context the constraint validator context
     * @return {@code true} if the request is valid with respect to this constraint
     */
    @Override
    public boolean isValid(final CreateMealEntryRequest value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.foodId() != null) {
            return true;
        }
        if (value.description() != null && !value.description().isBlank()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("description")
                .addConstraintViolation();
        return false;
    }
}
