/* (C)2025-2026 */
package com.univers.univers_backend.exception;

import com.univers.univers_backend.config.ApiResponse;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoSuchElementException(
            NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiResponse.error(
                                HttpStatus.NOT_FOUND.value(),
                                "Resource not found",
                                ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.error(
                                HttpStatus.BAD_REQUEST.value(), "Invalid input", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(
            IllegalStateException ex) {
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.error(
                                HttpStatus.BAD_REQUEST.value(), "Illegal state", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.error(
                                HttpStatus.FORBIDDEN.value(), "Access denied", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Object>> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.error(
                                HttpStatus.FORBIDDEN.value(),
                                "Operation not allowed",
                                ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiResponse.error(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Invalid credentials",
                                ex.getMessage()));
    }

    @ExceptionHandler({MissingCsrfTokenException.class, InvalidCsrfTokenException.class})
    public ResponseEntity<ApiResponse<Object>> handleCsrfException(CsrfException ex) {
        logger.warn("CSRF token validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.error(
                                HttpStatus.FORBIDDEN.value(),
                                "CSRF token validation failed",
                                "The CSRF token is missing or invalid. Please refresh the page"
                                        + " and try again."));
    }

    @ExceptionHandler(CsrfException.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericCsrfException(CsrfException ex) {
        logger.warn("CSRF error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.error(
                                HttpStatus.FORBIDDEN.value(),
                                "CSRF validation error",
                                ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String errorMessage =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.error(
                                HttpStatus.BAD_REQUEST.value(), "Validation failed", errorMessage));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String typeName =
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String error =
                String.format(
                        "The parameter '%s' of value '%s' could not be converted to type '%s'",
                        ex.getName(), ex.getValue(), typeName);
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.error(
                                HttpStatus.BAD_REQUEST.value(), "Invalid parameter type", error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        logger.error("An unexpected error occurred: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.error(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "An unexpected error occurred",
                                "Please contact support for assistance."));
    }
}
