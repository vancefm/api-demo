package com.demo.domain.batch;

import com.demo.domain.computersystem.ComputerSystemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for batch operations.
 *
 * Provides summary of batch operation results:
 * - List of successfully processed items
 * - Operation statistics (total, success count, failure count)
 * - Timestamp of operation
 * - Status of batch operation
 *
 * Example success response (HTTP 201):
 * {
 *   "items": [
 *     {"id": 1, "hostname": "SERVER-001", ...},
 *     {"id": 2, "hostname": "SERVER-002", ...}
 *   ],
 *   "totalItems": 2,
 *   "successCount": 2,
 *   "failureCount": 0,
 *   "status": "SUCCESS",
 *   "timestamp": "2025-11-28T10:30:00"
 * }
 *
 * Note: Failure responses return error details in GlobalExceptionHandler response,
 * not this DTO. This is only for successful operations.
 *
 * @see BatchComputerSystemRequest for input structure
 * @see BatchComputerSystemController for usage
 */
@Schema(description = "Response for batch computer system operations")
public class BatchComputerSystemResponse {

    @Schema(description = "List of processed items")
    private List<ComputerSystemDto> items;

    @Schema(description = "Total items in batch")
    private int totalItems;

    @Schema(description = "Successfully processed items", example = "2")
    private int successCount;

    @Schema(description = "Failed items", example = "0")
    private int failureCount;

    @Schema(description = "Batch operation status", example = "SUCCESS", 
            allowableValues = {"SUCCESS", "PARTIAL", "FAILED"})
    private String status;

    @Schema(description = "Operation timestamp")
    private LocalDateTime timestamp;

    // Constructors
    public BatchComputerSystemResponse() {
    }

    public BatchComputerSystemResponse(List<ComputerSystemDto> items, int totalItems, 
                                      int successCount, int failureCount, String status) {
        this.items = items;
        this.totalItems = totalItems;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public List<ComputerSystemDto> getItems() {
        return items;
    }

    public void setItems(List<ComputerSystemDto> items) {
        this.items = items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ComputerSystemDto> items;
        private int totalItems;
        private int successCount;
        private int failureCount;
        private String status;

        public Builder items(List<ComputerSystemDto> items) {
            this.items = items;
            return this;
        }

        public Builder totalItems(int totalItems) {
            this.totalItems = totalItems;
            return this;
        }

        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failureCount(int failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public BatchComputerSystemResponse build() {
            return new BatchComputerSystemResponse(items, totalItems, successCount, failureCount, status);
        }
    }
}
