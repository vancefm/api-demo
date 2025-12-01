package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.application.computersystem.ComputerSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/computer-systems")
@Tag(name = "Computer Systems", description = "APIs for managing computer systems")
public class ComputerSystemController {

    private final ComputerSystemService computerSystemService;

    public ComputerSystemController(ComputerSystemService computerSystemService) {
        this.computerSystemService = computerSystemService;
    }

    @PostMapping
    @Operation(summary = "Create a new computer system",
               description = "Creates a new computer system with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Computer system created successfully",
                     content = @Content(schema = @Schema(implementation = ComputerSystemDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Duplicate resource")
    })
    public ResponseEntity<ComputerSystemDto> createComputerSystem(
            @Valid @RequestBody ComputerSystemDto dto) {
        ComputerSystemDto created = computerSystemService.createComputerSystem(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get computer system by ID",
               description = "Retrieves a single computer system by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Computer system found",
                     content = @Content(schema = @Schema(implementation = ComputerSystemDto.class))),
        @ApiResponse(responseCode = "404", description = "Computer system not found")
    })
    public ResponseEntity<ComputerSystemDto> getComputerSystemById(
            @PathVariable Long id) {
        ComputerSystemDto computerSystem = computerSystemService.getComputerSystemById(id);
        return ResponseEntity.ok(computerSystem);
    }

    @GetMapping("/hostname/{hostname}")
    @Operation(summary = "Get computer system by hostname",
               description = "Retrieves a single computer system by its hostname")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Computer system found",
                     content = @Content(schema = @Schema(implementation = ComputerSystemDto.class))),
        @ApiResponse(responseCode = "404", description = "Computer system not found")
    })
    public ResponseEntity<ComputerSystemDto> getComputerSystemByHostname(
            @PathVariable String hostname) {
        ComputerSystemDto computerSystem = computerSystemService.getComputerSystemByHostname(hostname);
        return ResponseEntity.ok(computerSystem);
    }

    @GetMapping
    @Operation(summary = "Get all computer systems",
               description = "Retrieves all computer systems with pagination and sorting support")
    @ApiResponse(responseCode = "200", description = "List of computer systems retrieved",
                 content = @Content(schema = @Schema(implementation = ComputerSystemDto.class)))
    @Parameter(name = "page", description = "Page number (0-indexed)", example = "0", in = ParameterIn.QUERY)
    @Parameter(name = "size", description = "Page size", example = "20", in = ParameterIn.QUERY)
    @Parameter(name = "sort", description = "Sort criteria (e.g., 'id,desc')", example = "id,asc", in = ParameterIn.QUERY)
    public ResponseEntity<Page<ComputerSystemDto>> getAllComputerSystems(
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ComputerSystemDto> computerSystems = computerSystemService.getAllComputerSystems(pageable);
        return ResponseEntity.ok(computerSystems);
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter computer systems",
               description = "Filters computer systems based on hostname, department, and user with pagination and sorting")
    @ApiResponse(responseCode = "200", description = "Filtered computer systems retrieved")
    @Parameter(name = "page", description = "Page number (0-indexed)", example = "0", in = ParameterIn.QUERY)
    @Parameter(name = "size", description = "Page size", example = "20", in = ParameterIn.QUERY)
    @Parameter(name = "sort", description = "Sort criteria (e.g., 'id,desc')", example = "id,asc", in = ParameterIn.QUERY)
    public ResponseEntity<Page<ComputerSystemDto>> filterComputerSystems(
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String user,
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ComputerSystemDto> computerSystems = computerSystemService.filterComputerSystems(
                hostname, department, user, pageable);
        return ResponseEntity.ok(computerSystems);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update computer system",
               description = "Updates an existing computer system by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Computer system updated successfully",
                     content = @Content(schema = @Schema(implementation = ComputerSystemDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Computer system not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate resource")
    })
    public ResponseEntity<ComputerSystemDto> updateComputerSystem(
            @PathVariable Long id,
            @Valid @RequestBody ComputerSystemDto dto) {
        ComputerSystemDto updated = computerSystemService.updateComputerSystem(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete computer system",
               description = "Deletes a computer system by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Computer system deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Computer system not found")
    })
    public ResponseEntity<Void> deleteComputerSystem(
            @PathVariable Long id) {
        computerSystemService.deleteComputerSystem(id);
        return ResponseEntity.noContent().build();
    }
}
