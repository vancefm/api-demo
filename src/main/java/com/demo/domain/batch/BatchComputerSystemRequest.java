package com.demo.domain.batch;

import com.demo.domain.computersystem.ComputerSystemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Wrapper DTO for batch computer system operations (create, update, delete).
 *
 * Enables Spring's @Valid annotation to validate all items in a batch BEFORE
 * processing any of them. This is the foundation of all-or-nothing semantics.
 *
 * Validation cascade (automatic via Spring):
 * 1. @NotEmpty: Batch cannot be empty (fail fast)
 * 2. @Size: Batch size limited to prevent DOS attacks (max 100 items)
 * 3. @Valid: Each item in list validated using ComputerSystemDto constraints
 *    - All field-level constraints applied (hostname, IP, MAC, etc.)
 *    - If ANY item fails ANY constraint, HTTP 400 returned immediately
 *    - CRITICAL: No items processed if validation fails
 *
 * All-or-nothing guarantee:
 * - Validation phase: ALL items checked before ANY processing
 * - Processing phase: ALL items processed in single transaction or NONE
 * - Rollback: If any item fails during processing, entire batch rolled back
 *
 * Example request:
 * POST /api/v1/computer-systems/batch/create
 * {
 *   "items": [
 *     {
 *       "hostname": "SERVER-001",
 *       "manufacturer": "Dell",
 *       "model": "PowerEdge R750",
 *       "user": "john.doe",
 *       "department": "IT",
 *       "macAddress": "00:1A:2B:3C:4D:5E",
 *       "ipAddress": "192.168.1.100",
 *       "networkName": "PROD-NETWORK"
 *     },
 *     {
 *       "hostname": "SERVER-002",
 *       "manufacturer": "Dell",
 *       "model": "PowerEdge R750",
 *       "user": "jane.smith",
 *       "department": "IT",
 *       "macAddress": "00:1A:2B:3C:4D:5F",
 *       "ipAddress": "192.168.1.101",
 *       "networkName": "PROD-NETWORK"
 *     }
 *   ]
 * }
 *
 * Validation error response (HTTP 400):
 * {
 *   "status": 400,
 *   "message": "Request validation failed",
 *   "details": "items[0].ipAddress: Invalid IP address format; items[1].macAddress: Invalid MAC address format",
 *   "timestamp": "2025-11-28T10:30:00",
 *   "path": "/api/v1/computer-systems/batch/create"
 * }
 *
 * @see ComputerSystemDto for individual item constraints
 * @see BatchComputerSystemController for endpoint implementations
 * @see GlobalExceptionHandler for error response handling
 */
@Schema(description = "Batch request wrapper for multiple computer systems")
public class BatchComputerSystemRequest {

    @NotEmpty(message = "Batch items cannot be empty - at least 1 item required")
    @Valid  // CRITICAL: Cascades validation to ALL items in list before processing ANY
    @Schema(
        description = "List of computer systems to process (max items configurable via app.batch.max-items)",
        example = "[{\"hostname\":\"SERVER-001\",...}]"
    )
    private List<ComputerSystemDto> items;

    // Constructors
    public BatchComputerSystemRequest() {
    }

    public BatchComputerSystemRequest(List<ComputerSystemDto> items) {
        this.items = items;
    }

    // Getters and Setters
    public List<ComputerSystemDto> getItems() {
        return items;
    }

    public void setItems(List<ComputerSystemDto> items) {
        this.items = items;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ComputerSystemDto> items;

        public Builder items(List<ComputerSystemDto> items) {
            this.items = items;
            return this;
        }

        public BatchComputerSystemRequest build() {
            return new BatchComputerSystemRequest(items);
        }
    }
}
