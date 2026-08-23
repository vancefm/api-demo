package com.demo.feature.user;

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
 * REST controller for user management.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @Operation(summary = "Create a new user", description = "Create a new user in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto dto) {
        UserDto created = userManagementService.createUser(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve all users with pagination")
    @Parameter(name = "page", description = "Zero-indexed page number", in = ParameterIn.QUERY, example = "0")
    @Parameter(name = "size", description = "Page size", in = ParameterIn.QUERY, example = "20")
    @Parameter(name = "sort", description = "Sort field and direction (e.g. username,asc)", in = ParameterIn.QUERY, example = "id,asc")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userManagementService.getAllUsers(pageable));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter users",
        description = "Filters users by partial username/email match, department membership, and/or manager with pagination and sorting")
    @ApiResponse(responseCode = "200", description = "Filtered users retrieved")
    @Parameter(name = "page", description = "Zero-indexed page number", in = ParameterIn.QUERY, example = "0")
    @Parameter(name = "size", description = "Page size", in = ParameterIn.QUERY, example = "20")
    @Parameter(name = "sort", description = "Sort field and direction (e.g. username,asc)", in = ParameterIn.QUERY, example = "id,asc")
    public ResponseEntity<Page<UserDto>> filterUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long managerId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userManagementService.filterUsers(username, email, departmentId, managerId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userManagementService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto dto) {
        UserDto updated = userManagementService.updateUser(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
