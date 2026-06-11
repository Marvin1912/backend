package com.marvin.nutrition.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint requiring a non-blank {@code description} for ad-hoc meal entries
 * (i.e. when {@code foodId} is {@code null}). Food-backed entries are not affected.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AdHocDescriptionRequiredValidator.class)
public @interface AdHocDescriptionRequired {

    /**
     * The validation error message.
     *
     * @return the message template
     */
    String message() default "description must not be blank for ad-hoc entries";

    /**
     * Validation groups this constraint belongs to.
     *
     * @return the groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients of the Bean Validation API.
     *
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
}
