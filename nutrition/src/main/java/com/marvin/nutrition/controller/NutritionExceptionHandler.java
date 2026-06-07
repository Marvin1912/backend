package com.marvin.nutrition.controller;

import com.marvin.nutrition.service.TargetCalculationException;
import jakarta.persistence.EntityNotFoundException;
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
     * @param ex the exception
     * @return 409 response indicating a duplicate weight entry for the same date
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("A weight entry for this date already exists");
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
}
