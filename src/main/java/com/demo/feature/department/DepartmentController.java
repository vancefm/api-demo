package com.demo.feature.department;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for department management.
 */
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments", description = "APIs for managing departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @Operation(summary = "Create a new department", description = "Create a new department in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Department created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Department already exists")
    })
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        DepartmentDto created = departmentService.createDepartment(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieve a specific department by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Department found"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping
    @Operation(summary = "Get all departments", description = "Retrieve all departments with pagination")
    @Parameter(name = "page", description = "Zero-indexed page number", in = ParameterIn.QUERY, example = "0")
    @Parameter(name = "size", description = "Page size", in = ParameterIn.QUERY, example = "20")
    @Parameter(name = "sort", description = "Sort field and direction (e.g. name,asc)", in = ParameterIn.QUERY, example = "id,asc")
    public ResponseEntity<Page<DepartmentDto>> getAllDepartments(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(departmentService.getAllDepartments(pageable));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter departments",
        description = "Filters departments by partial name and/or description match with pagination and sorting")
    @ApiResponse(responseCode = "200", description = "Filtered departments retrieved")
    @Parameter(name = "page", description = "Zero-indexed page number", in = ParameterIn.QUERY, example = "0")
    @Parameter(name = "size", description = "Page size", in = ParameterIn.QUERY, example = "20")
    @Parameter(name = "sort", description = "Sort field and direction (e.g. name,asc)", in = ParameterIn.QUERY, example = "id,asc")
    public ResponseEntity<Page<DepartmentDto>> filterDepartments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(departmentService.filterDepartments(name, description, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department", description = "Update an existing department")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Department updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Department not found"),
        @ApiResponse(responseCode = "409", description = "Department name already exists")
    })
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentDto dto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete department",
        description = "Delete a department. Any users or computer systems assigned to it are silently dissociated.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Department deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
