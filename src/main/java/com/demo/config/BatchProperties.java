package com.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for batch operations.
 *
 * Centralizes all batch-related configuration in a type-safe, reusable class.
 * Properties are loaded from application.yml under the "app.batch" prefix.
 *
 * Benefits:
 * - Type-safe: No string keys or casting required
 * - Centralized: Single source of truth for batch config
 * - Externalized: Can be changed in application.yml without recompiling
 * - Testable: Easy to inject mock configuration for testing
 * - Documented: Comments explain each property's purpose
 *
 * Example application.yml configuration:
 * app:
 *   batch:
 *     max-items: 100
 *     timeout-seconds: 300
 *
 * Usage in controller:
 * @Autowired
 * private BatchProperties batchProperties;
 *
 * public void processBatch(List<?> items) {
 *     if (items.size() > batchProperties.getMaxItems()) {
 *         throw new BatchSizeExceededException(...);
 *     }
 * }
 *
 * @see BatchComputerSystemController for usage
 */
@Configuration
@ConfigurationProperties(prefix = "app.batch")
public class BatchProperties {

    /**
     * Maximum number of items allowed in a single batch operation.
     *
     * Prevents DOS attacks by limiting batch size. Set based on:
     * - Database transaction limits
     * - Memory constraints
     * - Expected processing time
     * - Performance requirements
     *
     * Default: 100 (reasonable for most applications)
     * Typical range: 10-1000 depending on item complexity
     *
     * Example values:
     * - Small/memory-constrained: 10-50
     * - Typical application: 50-200
     * - High-performance with large items: 100-1000
     */
    private int maxItems = 100;

    /**
     * Timeout for batch operations in seconds.
     *
     * Maximum time allowed for a batch to complete.
     * If batch takes longer, it's terminated and rolled back.
     *
     * Default: 300 (5 minutes)
     * Typical range: 30-600 seconds depending on item complexity
     *
     * Set based on:
     * - Expected processing time for max batch
     * - Database timeout settings
     * - Network timeout settings
     */
    private int timeoutSeconds = 300;

    // Constructors
    public BatchProperties() {
    }

    // Getters and Setters
    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        if (maxItems < 1) {
            throw new IllegalArgumentException("Batch max items must be at least 1");
        }
        if (maxItems > 10000) {
            throw new IllegalArgumentException("Batch max items cannot exceed 10000 (DOS protection)");
        }
        this.maxItems = maxItems;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("Batch timeout must be at least 1 second");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String toString() {
        return "BatchProperties{" +
                "maxItems=" + maxItems +
                ", timeoutSeconds=" + timeoutSeconds +
                '}';
    }
}
