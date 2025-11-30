package com.demo.shared.exception;

import com.demo.application.computersystem.EmailNotificationService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler for API error responses using RFC 9457 ProblemDetail.
 *
 * Uses Spring's ProblemDetail (RFC 9457) for standardized error responses.
 * All exceptions are converted to structured ProblemDetail responses with proper HTTP status codes.
 * Also integrates with email service (protected by circuit breaker) to notify admins of critical errors.
 *
 * Exception handling flow:
 * 1. Exception thrown by controller or service
 * 2. GlobalExceptionHandler catches it
 * 3. RFC 9457 ProblemDetail created
 * 4. Email notification sent (if appropriate and email service available)
 * 5. ProblemDetail response returned to client
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
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource Not Found");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

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

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Handles DuplicateResourceException (HTTP 409).
     * Request attempted to create resource with duplicate unique identifier
     * (e.g., hostname, MAC address, or IP already exists).
     *
     * Always emails duplicate errors as these indicate potential data issues.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Duplicate Resource");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

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

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
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
    public ResponseEntity<ProblemDetail> handleValidationErrors(
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

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Request Validation Failed");
        problem.setDetail(errors);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        // Log validation errors for debugging
        logger.debug("Validation error for {}: {}", request.getRequestURI(), errors);

        // Don't email validation errors - these are client mistakes, not server issues
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles CallNotPermittedException (HTTP 503).
     * Circuit breaker is OPEN, external service unavailable.
     * Returns Service Unavailable instead of attempting operation.
     *
     * Alerted through circuit breaker fallback methods, not here.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitBreakerOpen(
            CallNotPermittedException ex,
            HttpServletRequest request) {
        
        logger.warn("Circuit breaker is OPEN for request to: {}", request.getRequestURI());
        
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Service Temporarily Unavailable");
        problem.setDetail("A critical service is currently unavailable. Please try again in a moment.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    /**
     * Handles all other unexpected exceptions (HTTP 500).
     * Logs full exception and sends critical alert email to admin.
     *
     * This is the catch-all handler for any unexpected server errors.
     * Always emails critical errors so admin can investigate.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {
        
        // Log full stack trace for debugging
        logger.error("Unhandled exception in API: {}", request.getRequestURI(), ex);
        
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

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

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
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