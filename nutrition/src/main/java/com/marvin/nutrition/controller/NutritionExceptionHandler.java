package com.marvin.nutrition.controller;

import com.marvin.nutrition.service.BarcodeLookupException;
import com.marvin.nutrition.service.LabelReadException;
import com.marvin.nutrition.service.MealEstimateException;
import com.marvin.nutrition.service.TargetCalculationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Global exception handler for the nutrition module REST controllers. */
@RestControllerAdvice(basePackages = "com.marvin.nutrition")
public class NutritionExceptionHandler {

    /**
     * Maps {@link NoSuchElementException} to HTTP 404.
     *
     * @param ex the exception
     * @return 404 response with the exception message
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Maps {@link EntityNotFoundException} to HTTP 404.
     *
     * @param ex the exception
     * @return 404 response with the exception message
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Maps {@link DataIntegrityViolationException} to HTTP 409 Conflict.
     *
     * <p>If the violation originates from the {@code weight_entry} unique date constraint, a
     * weight-specific message is returned. For any other constraint violation (e.g. day-target
     * snapshot or profile singleton constraints), a generic conflict message is returned so
     * callers are not misled.</p>
     *
     * @param ex the exception
     * @return 409 response with a message describing the conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        if (isWeightEntryDateConflict(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A weight entry for this date already exists");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("The request conflicts with existing data.");
    }

    /**
     * Determines whether the given exception was caused by a violation of the
     * {@code weight_entry} table's unique entry-date constraint.
     *
     * @param ex the exception to inspect
     * @return {@code true} if the cause is a Hibernate constraint violation on the weight entry date
     */
    private boolean isWeightEntryDateConflict(DataIntegrityViolationException ex) {
        final Throwable cause = ex.getCause();
        if (!(cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolation)) {
            return false;
        }
        final String constraintName = constraintViolation.getConstraintName();
        return constraintName != null && constraintName.startsWith("weight_entry");
    }

    /**
     * Maps {@link TargetCalculationException} to HTTP 400.
     *
     * @param ex the exception
     * @return 400 response with an explanatory message
     */
    @ExceptionHandler(TargetCalculationException.class)
    public ResponseEntity<String> handleTargetCalculation(TargetCalculationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * Maps {@link LabelReadException} to HTTP 422 Unprocessable Entity.
     *
     * @param ex the exception indicating the label image could not be parsed
     * @return 422 response with the exception message
     */
    @ExceptionHandler(LabelReadException.class)
    public ResponseEntity<String> handleLabelRead(LabelReadException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }

    /**
     * Maps {@link MealEstimateException} to HTTP 422 Unprocessable Entity.
     *
     * @param ex the exception indicating the meal macro estimation failed
     * @return 422 response with the exception message
     */
    @ExceptionHandler(MealEstimateException.class)
    public ResponseEntity<String> handleMealEstimate(MealEstimateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }

    /**
     * Maps {@link BarcodeLookupException} to HTTP 422 Unprocessable Entity.
     *
     * @param ex the exception indicating the barcode was found but returned no usable nutrition data
     * @return 422 response with the exception message
     */
    @ExceptionHandler(BarcodeLookupException.class)
    public ResponseEntity<String> handleBarcodeLookup(BarcodeLookupException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }

    /**
     * Maps {@link IllegalArgumentException} to HTTP 400.
     *
     * @param ex the exception
     * @return 400 response with the exception message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * Maps bean-validation failures ({@link MethodArgumentNotValidException}) to HTTP 400.
     *
     * @param ex the exception carrying field error details
     * @return 400 response with comma-separated field error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        final String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Maps method-parameter constraint violations ({@link ConstraintViolationException}) to HTTP 400.
     *
     * @param ex the exception carrying constraint violation details
     * @return 400 response with comma-separated constraint violation messages
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
        final String errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(errors);
    }
}
