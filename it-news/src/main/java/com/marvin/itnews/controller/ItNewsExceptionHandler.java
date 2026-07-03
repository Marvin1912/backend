package com.marvin.itnews.controller;

import jakarta.persistence.EntityNotFoundException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

/** Global exception handler for the it-news module REST controllers. */
@RestControllerAdvice(basePackages = "com.marvin.itnews")
public class ItNewsExceptionHandler {

    /**
     * Maps {@link EntityNotFoundException} to HTTP 404.
     *
     * @param ex the exception
     * @return 404 response with the exception message
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Maps {@link DataIntegrityViolationException} to HTTP 409 Conflict.
     *
     * @param ex the exception
     * @return 409 response with a message describing the conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("A feed config with this URL already exists");
    }

    /**
     * Maps bean-validation failures on {@code @Valid @RequestBody} arguments
     * ({@link WebExchangeBindException}) to HTTP 400.
     *
     * <p>WebFlux annotated controllers raise {@link WebExchangeBindException} for these failures,
     * not the Spring MVC {@code MethodArgumentNotValidException}.</p>
     *
     * @param ex the exception carrying field error details
     * @return 400 response with comma-separated field error messages
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<String> handleValidation(WebExchangeBindException ex) {
        final String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(errors);
    }

}
