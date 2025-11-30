package com.demo.controller;

import com.demo.config.BatchProperties;
import com.demo.dto.BatchComputerSystemRequest;
import com.demo.dto.BatchComputerSystemResponse;
import com.demo.dto.ComputerSystemDto;
import com.demo.service.ComputerSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Batch operations controller for computer systems.
 *
 * Implements all-or-nothing batch processing following enterprise best practices:
 *
 * ALL-OR-NOTHING GUARANTEE:
 * 1. VALIDATION PHASE (Spring @Valid): ALL items validated before ANY processing
 *    - Each item checked against ComputerSystemDto constraints
 *    - If ANY item fails validation, HTTP 400 returned immediately
 *    - Zero items created/updated if validation fails (fail-fast)
 *
 * 2. PRE-PROCESSING PHASE (Optional): Additional business logic validation
 *    - Check for duplicates, conflicts, constraints
 *    - Prevent partial batch execution
 *
 * 3. PROCESSING PHASE (@Transactional): All items processed in single transaction
 *    - All items created/updated in database
 *    - If ANY item fails during processing, entire transaction rolled back
 *    - Either ALL items processed or NONE (database guarantees atomicity)
 *
 * BENEFITS:
 * - Data consistency: No partial updates or inconsistent state
 * - Fail-fast validation: Detect problems before touching database
 * - Clear semantics: Client knows batch either fully succeeds or fully fails
 * - Transactional safety: ACID guarantees from database
 *
 * USAGE EXAMPLES:
 * - Bulk server deployment: Create 100 new servers or create none
 * - Bulk configuration: Update 50 servers to new config or update none
 * - Bulk decommission: Delete 25 retired servers or delete none
 *
 * @see BatchComputerSystemRequest for validation structure
 * @see ComputerSystemService for transactional processing
 * @see GlobalExceptionHandler for error handling
 */
@RestController
@RequestMapping("/api/v1/computer-systems/batch")
@Tag(name = "Batch Operations", 
     description = "Bulk create, update, and delete operations with all-or-nothing guarantees")
public class BatchComputerSystemController {

    private static final Logger logger = LoggerFactory.getLogger(BatchComputerSystemController.class);
    private final ComputerSystemService computerSystemService;
    private final BatchProperties batchProperties;

    public BatchComputerSystemController(ComputerSystemService computerSystemService,
                                       BatchProperties batchProperties) {
        this.computerSystemService = computerSystemService;
        this.batchProperties = batchProperties;
    }

    /**
     * Batch create multiple computer systems.
     *
     * ALL-OR-NOTHING FLOW:
     * 1. VALIDATION (Spring @Valid): 
     *    - Validates ALL items in list
     *    - Returns HTTP 400 if ANY item invalid
     *    - No database changes if validation fails
     *
     * 2. PROCESSING (@Transactional):
     *    - Creates all items in single database transaction
     *    - If ANY item creation fails (e.g., duplicate), transaction rolls back
     *    - Either ALL items created or NONE
     *
     * @param request BatchComputerSystemRequest with list of items
     *                Spring automatically validates with @Valid annotation
     * @return 201 Created with BatchComputerSystemResponse containing created items
     * @throws MethodArgumentNotValidException if validation fails (HTTP 400)
     * @throws DuplicateResourceException if duplicate detected (HTTP 409, transaction rolled back)
     *
     * EXAMPLE REQUEST:
     * POST /api/v1/computer-systems/batch/create
     * Content-Type: application/json
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
     * SUCCESS RESPONSE (HTTP 201):
     * {
     *   "items": [
     *     {"id": 101, "hostname": "SERVER-001", ...},
     *     {"id": 102, "hostname": "SERVER-002", ...}
     *   ],
     *   "totalItems": 2,
     *   "successCount": 2,
     *   "failureCount": 0,
     *   "status": "SUCCESS",
     *   "timestamp": "2025-11-28T10:30:00"
     * }
     *
     * VALIDATION ERROR (HTTP 400):
     * {
     *   "status": 400,
     *   "message": "Request validation failed",
     *   "details": "items[0].ipAddress: Invalid IP address format",
     *   "path": "/api/v1/computer-systems/batch/create"
     * }
     *
     * DUPLICATE ERROR (HTTP 409, NO ITEMS CREATED):
     * {
     *   "status": 409,
     *   "message": "Duplicate resource",
     *   "details": "Hostname 'SERVER-001' already exists in the system",
     *   "path": "/api/v1/computer-systems/batch/create"
     * }
     */
    @PostMapping("/create")
    @Operation(
        summary = "Batch create computer systems",
        description = "Create multiple computer systems in a single transaction. Validates all items " +
                     "before creating any. If validation fails OR any creation fails, NO items are created."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "All items created successfully",
                     content = @Content(schema = @Schema(implementation = BatchComputerSystemResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed or batch size exceeded"),
        @ApiResponse(responseCode = "409", description = "Duplicate resource detected - no items created")
    })
    @Transactional  // Ensures all-or-nothing: all created or none created
    public ResponseEntity<Object> batchCreate(
            @Valid @RequestBody BatchComputerSystemRequest request,
            HttpServletRequest httpRequest) {

        int batchSize = request.getItems().size();
        logger.info("Batch create started: {} items", batchSize);

        // Validate batch size against configured maximum
        ProblemDetail sizeError = validateBatchSize(batchSize, httpRequest.getRequestURI());
        if (sizeError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(sizeError);
        }

        try {
            // All items passed Spring validation (@Valid on request.items)
            // Now process in transaction - if any fails, entire transaction rolls back

            List<ComputerSystemDto> createdItems = request.getItems().stream()
                    .map(computerSystemService::createComputerSystem)
                    .collect(Collectors.toList());

            logger.info("Batch create completed successfully: {} items created", batchSize);

            // Build success response
            BatchComputerSystemResponse response = BatchComputerSystemResponse.builder()
                    .items(createdItems)
                    .totalItems(batchSize)
                    .successCount(batchSize)
                    .failureCount(0)
                    .status("SUCCESS")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception ex) {
            // Transaction automatically rolled back by Spring @Transactional
            // No items created if any failure during processing
            logger.error("Batch create failed - transaction rolled back, {} items NOT created", batchSize, ex);
            throw ex;
        }
    }

    /**
     * Batch update multiple computer systems.
     *
     * ALL-OR-NOTHING FLOW:
     * 1. VALIDATION (Spring @Valid):
     *    - Validates ALL items in list
     *    - Returns HTTP 400 if ANY item invalid
     *
     * 2. PROCESSING (@Transactional):
     *    - Updates all items in single transaction
     *    - If ANY item update fails, transaction rolls back
     *    - Either ALL items updated or NONE
     *
     * DATA CONSISTENCY EXAMPLES:
     * - Scenario: Update 50 servers' department to "DevOps"
     *   Result: ALL 50 updated or NONE updated (no partial updates)
     * - Scenario: Update network config on 30 servers
     *   Result: ALL 30 get new config or ALL keep old config
     * - Scenario: Update security group for 100 servers
     *   Result: ALL protected or ALL unchanged
     *
     * @param request BatchComputerSystemRequest with items to update
     * @return 200 OK with BatchComputerSystemResponse
     * @throws MethodArgumentNotValidException if validation fails (HTTP 400)
     *
     * EXAMPLE REQUEST:
     * PUT /api/v1/computer-systems/batch/update
     * {
     *   "items": [
     *     {"id": 1, "hostname": "SERVER-001", ..., "department": "DevOps"},
     *     {"id": 2, "hostname": "SERVER-002", ..., "department": "DevOps"}
     *   ]
     * }
     *
     * RESULT: Both updated to DevOps, or neither updated if any error
     */
    @PutMapping("/update")
    @Operation(
        summary = "Batch update computer systems",
        description = "Update multiple computer systems in a single transaction. Validates all items " +
                     "before updating any. If validation fails OR any update fails, NO items are updated."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All items updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed or batch size exceeded"),
        @ApiResponse(responseCode = "404", description = "One or more items not found - no items updated")
    })
    @Transactional  // Ensures all-or-nothing: all updated or none updated
    public ResponseEntity<Object> batchUpdate(
            @Valid @RequestBody BatchComputerSystemRequest request,
            HttpServletRequest httpRequest) {

        int batchSize = request.getItems().size();
        logger.info("Batch update started: {} items", batchSize);

        // Validate batch size against configured maximum
        ProblemDetail sizeError = validateBatchSize(batchSize, httpRequest.getRequestURI());
        if (sizeError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(sizeError);
        }

        try {
            // All items passed Spring validation
            // Process in transaction

            List<ComputerSystemDto> updatedItems = request.getItems().stream()
                    .map(item -> computerSystemService.updateComputerSystem(item.getId(), item))
                    .collect(Collectors.toList());

            logger.info("Batch update completed successfully: {} items updated", batchSize);

            // Build success response
            BatchComputerSystemResponse response = BatchComputerSystemResponse.builder()
                    .items(updatedItems)
                    .totalItems(batchSize)
                    .successCount(batchSize)
                    .failureCount(0)
                    .status("SUCCESS")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            // Transaction automatically rolled back by Spring @Transactional
            // No items updated if any failure during processing
            logger.error("Batch update failed - transaction rolled back, {} items NOT updated", batchSize, ex);
            throw ex;
        }
    }

    /**
     * Batch delete multiple computer systems by ID.
     *
     * ALL-OR-NOTHING FLOW:
     * 1. VALIDATION (Spring @Valid): Validates request structure
     * 2. PRE-DELETE VERIFICATION: Verify all IDs exist before deleting ANY
     *    - Two-phase approach prevents "deleted 3 of 5"
     *    - If ANY ID not found, HTTP 404 returned, NO items deleted
     * 3. PROCESSING (@Transactional):
     *    - Deletes all items in single transaction
     *    - If ANY delete fails, transaction rolls back
     *    - Either ALL deleted or NONE deleted
     *
     * TWO-PHASE SAFETY:
     * Phase 1: Verify all IDs exist (ResourceNotFoundException if any not found)
     * Phase 2: Delete all in transaction (rollback if any fails)
     * This prevents "successfully deleted 3 items, but couldn't delete item 4"
     *
     * @param request BatchComputerSystemRequest with items to delete
     *                Only uses item IDs; other fields ignored
     * @return 204 No Content (successful deletion)
     * @throws ResourceNotFoundException if any ID not found (HTTP 404, NO items deleted)
     *
     * EXAMPLE REQUEST:
     * DELETE /api/v1/computer-systems/batch/delete
     * {
     *   "items": [
     *     {"id": 1},
     *     {"id": 2},
     *     {"id": 3}
     *   ]
     * }
     *
     * SUCCESS (HTTP 204): All 3 deleted
     * ERROR (HTTP 404): Item not found - NO items deleted (transaction rolled back)
     */
    @DeleteMapping("/delete")
    @Operation(
        summary = "Batch delete computer systems",
        description = "Delete multiple computer systems in a single transaction. Verifies all items " +
                     "exist before deleting any. If any item not found, NO items are deleted."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "All items deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or batch size exceeded"),
        @ApiResponse(responseCode = "404", description = "One or more items not found - no items deleted")
    })
    @Transactional  // Ensures all-or-nothing: all deleted or none deleted
    public ResponseEntity<Object> batchDelete(
            @Valid @RequestBody BatchComputerSystemRequest request,
            HttpServletRequest httpRequest) {

        int batchSize = request.getItems().size();
        logger.info("Batch delete started: {} items", batchSize);

        // Validate batch size against configured maximum
        ProblemDetail sizeError = validateBatchSize(batchSize, httpRequest.getRequestURI());
        if (sizeError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(sizeError);
        }

        try {
            // Extract IDs from request
            List<Long> ids = request.getItems().stream()
                    .map(ComputerSystemDto::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            // TWO-PHASE APPROACH:
            // Phase 1: Verify all items exist BEFORE deleting any
            // This prevents "deleted 3 of 5" scenario
            logger.debug("Verifying {} items exist before deletion", ids.size());
            for (Long id : ids) {
                // Throws ResourceNotFoundException if not found
                computerSystemService.getComputerSystemById(id);
            }

            logger.debug("All {} items verified - proceeding with deletion", ids.size());

            // Phase 2: Delete all in transaction
            // If any delete fails, transaction rolls back and no items are deleted
            ids.forEach(computerSystemService::deleteComputerSystem);

            logger.info("Batch delete completed successfully: {} items deleted", ids.size());

            return ResponseEntity.noContent().build();

        } catch (Exception ex) {
            // Transaction automatically rolled back by Spring @Transactional
            // No items deleted if any verification or deletion fails
            logger.error("Batch delete failed - transaction rolled back, {} items NOT deleted", batchSize, ex);
            throw ex;
        }
    }

    /**
     * Validates batch size against configured maximum.
     *
     * Returns null if size is valid, or ProblemDetail if size exceeds limit.
     * This prevents DOS attacks by enforcing configurable batch size limits.
     *
     * @param batchSize Number of items in batch
     * @param uri Request URI for error response
     * @return ProblemDetail if batch size exceeds limit, null if valid
     */
    private ProblemDetail validateBatchSize(int batchSize, String uri) {
        if (batchSize > batchProperties.getMaxItems()) {
            String message = String.format(
                    "Batch size (%d) exceeds maximum (%d) - reduce batch size or increase app.batch.max-items configuration",
                    batchSize,
                    batchProperties.getMaxItems()
            );
            
            logger.warn("Batch size validation failed: {}", message);
            
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problem.setTitle("Batch Size Exceeds Maximum");
            problem.setDetail(message);
            problem.setInstance(URI.create(uri));
            problem.setProperty("timestamp", Instant.now());
            problem.setProperty("batchSize", batchSize);
            problem.setProperty("maxItems", batchProperties.getMaxItems());
            
            return problem;
        }
        return null;
    }
}
