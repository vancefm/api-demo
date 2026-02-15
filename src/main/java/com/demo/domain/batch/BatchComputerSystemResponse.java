package com.demo.domain.batch;

import com.demo.domain.computersystem.ComputerSystemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
