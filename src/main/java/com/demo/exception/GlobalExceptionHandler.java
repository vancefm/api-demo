package com.demo.exception;

import com.demo.service.EmailNotificationService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Global exception handler for API error responses.
 *
 * Handles all exceptions thrown by controllers and service layer,
 * returns structured error responses with proper HTTP status codes.
 * Also integrates with email service (protected by circuit breaker)
 * to notify admins of critical errors.
 *
 * Exception handling flow:
 * 1. Exception thrown by controller or service
 * 2. GlobalExceptionHandler catches it
 * 3. Structured ErrorResponse created
 * 4. Email notification sent (if appropriate and email service available)
 * 5. Error response returned to client
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final EmailNotificationService emailNotificationService;

    public GlobalExceptionHandler(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Handles ResourceNotFoundException (HTTP 404).
     * Resource was not found in database.
     *
     * Only emails non-critical 404 errors (resources that should exist).
     * Spam protection: doesn't email for normal 404 responses from bad requests.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                "Resource not found",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        // Only email if it's from a specific endpoint (optional)
        // Prevents spam from normal 404s
        if (shouldEmailError(request.getRequestURI())) {
            try {
                emailNotificationService.sendErrorNotification(
                        ex,
                        request.getRequestURI(),
                        "Method: " + request.getMethod() + "\nQuery: " + request.getQueryString()
                );
            } catch (Exception emailEx) {
                logger.warn("Failed to send error notification for 404", emailEx);
                // Don't let email failure impact API response
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles DuplicateResourceException (HTTP 409).
     * Request attempted to create resource with duplicate unique identifier
     * (e.g., hostname, MAC address, or IP already exists).
     *
     * Always emails duplicate errors as these indicate potential data issues.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                "Duplicate resource",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        // Always email duplicate errors - these are suspicious
        try {
            emailNotificationService.sendErrorNotification(
                    ex,
                    request.getRequestURI(),
                    "Method: " + request.getMethod() + "\nQuery: " + request.getQueryString()
            );
        } catch (Exception emailEx) {
            logger.warn("Failed to send error notification for duplicate resource", emailEx);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles MethodArgumentNotValidException (HTTP 400).
     * Request validation failed (missing fields, invalid types, validation rules).
     *
     * BATCH OPERATIONS SUPPORT:
     * For batch requests, provides item-level error details showing which items
     * failed validation and why. This is critical for all-or-nothing semantics
     * where the entire batch is rejected if ANY item fails validation.
     *
     * Example batch error:
     * "items[0].ipAddress: Invalid IP address format; items[1].macAddress: Invalid MAC address format"
     *
     * This tells client exactly which items in the batch are problematic, so they
     * can fix those specific items and retry the batch.
     *
     * Does NOT email validation errors (client mistake, not server issue).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        // Build detailed validation error message from ALL field errors
        // Supports batch operations with item-level error reporting
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> {
                    // Format: "fieldName: validationMessage"
                    // Examples:
                    // - "hostname: Hostname is required"
                    // - "items[0].ipAddress: Invalid IP address format"
                    // - "items[1].macAddress: Invalid MAC address format"
                    return e.getField() + ": " + e.getDefaultMessage();
                })
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                errors,
                LocalDateTime.now(),
                request.getRequestURI()
        );

        // Log validation errors for debugging
        logger.debug("Validation error for {}: {}", request.getRequestURI(), errors);

        // Don't email validation errors - these are client mistakes, not server issues
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles CallNotPermittedException (HTTP 503).
     * Circuit breaker is OPEN, external service unavailable.
     * Returns Service Unavailable instead of attempting operation.
     *
     * Alerted through circuit breaker fallback methods, not here.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex,
            HttpServletRequest request) {
        
        logger.warn("Circuit breaker is OPEN for request to: {}", request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service temporarily unavailable",
                "A critical service is currently unavailable. Please try again in a moment.",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions (HTTP 500).
     * Logs full exception and sends critical alert email to admin.
     *
     * This is the catch-all handler for any unexpected server errors.
     * Always emails critical errors so admin can investigate.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {
        
        // Log full stack trace for debugging
        logger.error("Unhandled exception in API: {}", request.getRequestURI(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        // Always email critical errors so admin can investigate
        // Protected by email service circuit breaker
        try {
            emailNotificationService.sendCriticalErrorAlert(
                    ex,
                    request.getRequestURI()
            );
        } catch (Exception emailEx) {
            // Email service might be down (circuit breaker fallback already logged)
            logger.error("Failed to send critical error alert email", emailEx);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Determines if an error should trigger email notification.
     * Prevents spam by excluding certain endpoints.
     *
     * @param uri Request URI
     * @return true if email should be sent for this error
     */
    private boolean shouldEmailError(String uri) {
        // Don't email errors from monitoring, docs, or health endpoints
        return !uri.contains("/actuator") && 
               !uri.contains("/swagger") && 
               !uri.contains("/h2-console") &&
               !uri.contains("/v3/api-docs");
    }
}