package com.demo.shared.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications with circuit breaker protection.
 *
 * The circuit breaker protects against email server failures:
 * - If email service fails repeatedly, circuit opens and prevents timeout delays
 * - Failed notifications are logged locally instead of waiting for SMTP timeout
 * - After recovery period, circuit enters HALF_OPEN state to test if service recovered
 *
 * Circuit breaker states:
 * - CLOSED: Normal operation, emails sent as expected
 * - OPEN: Email service unavailable, notifications logged locally instead
 * - HALF_OPEN: Testing recovery, limited emails sent to verify service is back up
 */
@Service
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final String adminEmail;
    private final String appName;
    private final String appEnvironment;

    public EmailNotificationService(
            JavaMailSender mailSender,
            @Value("${app.admin.email:admin@example.com}") String adminEmail,
            @Value("${app.name:Computer Systems API}") String appName,
            @Value("${app.environment:development}") String appEnvironment) {
        this.mailSender = mailSender;
        this.adminEmail = adminEmail;
        this.appName = appName;
        this.appEnvironment = appEnvironment;
    }

    /**
     * Sends error notification email with circuit breaker protection.
     *
     * @CircuitBreaker annotation:
     * - name: "emailService" - matches configuration in application.yml
     * - fallbackMethod: "sendErrorNotificationFallback" - called when circuit is OPEN
     *
     * If this method fails 50% of the last 10 calls, or calls are slow (>2s),
     * the circuit opens and fallback method is called instead.
     *
     * @param exception The exception that occurred
     * @param endpoint The API endpoint where error occurred
     * @param requestDetails Additional error details
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendErrorNotificationFallback")
    public void sendErrorNotification(Exception exception, String endpoint, String requestDetails) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setFrom(adminEmail);
            message.setSubject(buildSubject(exception));
            message.setText(buildEmailBody(exception, endpoint, requestDetails));

            mailSender.send(message);
            log.debug("Error notification email sent successfully for: {}", exception.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to send error notification email", e);
            // Exception will be caught by circuit breaker and fallback called
            throw new RuntimeException("Failed to send error notification via email", e);
        }
    }

    /**
     * Fallback method called when emailService circuit breaker is OPEN.
     * Email service is unavailable, so we log the notification locally instead.
     *
     * Method signature must match the original method but with additional
     * CallNotPermittedException parameter at the end.
     *
     * @param exception The original exception
     * @param endpoint The endpoint where error occurred
     * @param requestDetails Error details
     * @param circuitException The circuit breaker exception (circuit is OPEN)
     */
    public void sendErrorNotificationFallback(Exception exception, String endpoint, String requestDetails,
                                             CallNotPermittedException circuitException) {
        log.warn("========================================");
        log.warn("CIRCUIT BREAKER OPEN: Email service unavailable");
        log.warn("========================================");
        log.error("Error notification that would have been emailed:", exception);
        log.error("Endpoint: {}", endpoint);
        log.error("Details: {}", requestDetails);
        log.error("Admin would have been notified at: {}", adminEmail);
        log.warn("Notification stored in logs instead of sending email");
        log.warn("========================================");
        
        // In production, you could also:
        // - Write to database for later processing
        // - Send to alternative notification channel (SMS, Slack, etc.)
        // - Queue message for retry when service recovers
    }

    /**
     * Sends critical alert email with circuit breaker protection.
     * Used when system experiences critical errors.
     *
     * @param exception The critical exception
     * @param endpoint The API endpoint affected
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendCriticalErrorAlertFallback")
    public void sendCriticalErrorAlert(Exception exception, String endpoint) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setFrom(adminEmail);
            message.setSubject("🚨 CRITICAL ERROR - " + appName + " (" + appEnvironment + ")");
            message.setText(buildCriticalEmailBody(exception, endpoint));

            mailSender.send(message);
            log.warn("Critical error alert email sent");
        } catch (Exception e) {
            log.error("Failed to send critical error email", e);
            // Exception will be caught by circuit breaker and fallback called
            throw new RuntimeException("Failed to send critical alert via email", e);
        }
    }

    /**
     * Fallback for critical alert when email service is down.
     * This is called when we can't send critical alerts via email.
     *
     * @param exception The original critical exception
     * @param endpoint The affected endpoint
     * @param circuitException The circuit breaker exception
     */
    public void sendCriticalErrorAlertFallback(Exception exception, String endpoint,
                                              CallNotPermittedException circuitException) {
        log.error("========================================");
        log.error("CRITICAL: Cannot send alert email - email service is DOWN");
        log.error("Affected Endpoint: {}", endpoint);
        log.error("========================================");
        log.error("Critical exception:", exception);
        log.error("Admin should be notified immediately at: {}", adminEmail);
        log.error("========================================");
        
        // In production, send critical alert via alternative channel:
        // - Call admin phone number
        // - Send SMS alert
        // - Post to Slack/Teams channel
        // - Write to system event log
        // - Page on-call engineer
    }

    /**
     * Builds email subject line from exception type.
     */
    private String buildSubject(Exception exception) {
        return "⚠️ " + appName + " Error - " + exception.getClass().getSimpleName() + " [" + appEnvironment + "]";
    }

    /**
     * Builds detailed error email body with exception and context information.
     */
    private String buildEmailBody(Exception exception, String endpoint, String requestDetails) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return "Error Details:\n" +
                "=============\n" +
                "Timestamp: " + LocalDateTime.now().format(formatter) + "\n" +
                "Application: " + appName + "\n" +
                "Environment: " + appEnvironment + "\n" +
                "Endpoint: " + endpoint + "\n\n" +
                "Exception Details:\n" +
                "==================\n" +
                "Type: " + exception.getClass().getName() + "\n" +
                "Message: " + exception.getMessage() + "\n\n" +
                "Request Details:\n" +
                "================\n" +
                requestDetails + "\n\n" +
                "Stack Trace:\n" +
                "============\n" +
                getStackTrace(exception) + "\n\n" +
                "Please investigate this error immediately.\n" +
                "---\n" +
                "Automated Error Alert System";
    }

    /**
     * Builds critical error email body.
     */
    private String buildCriticalEmailBody(Exception exception, String endpoint) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return "🚨 CRITICAL ERROR OCCURRED 🚨\n\n" +
                "Timestamp: " + LocalDateTime.now().format(formatter) + "\n" +
                "Application: " + appName + "\n" +
                "Environment: " + appEnvironment + "\n" +
                "Endpoint: " + endpoint + "\n" +
                "Exception: " + exception.getClass().getSimpleName() + "\n" +
                "Message: " + exception.getMessage() + "\n\n" +
                "Stack Trace:\n" +
                "============\n" +
                getStackTrace(exception) + "\n\n" +
                "IMMEDIATE ACTION REQUIRED!\n" +
                "---\n" +
                "Automated Critical Alert System";
    }

    /**
     * Extracts stack trace from exception for email inclusion.
     */
    private String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        
        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
            sb.append("at ").append(stackTrace[i]).append("\n");
        }
        
        if (stackTrace.length > 10) {
            sb.append("... ").append(stackTrace.length - 10).append(" more\n");
        }
        
        return sb.toString();
    }
}
