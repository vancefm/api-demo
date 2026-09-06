package com.demo.feature.security.rbac;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for roles and their permissions.
 */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Roles are dynamic sets of entity:field CRUD permissions")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @Operation(summary = "Create a role",
        description = "Creates a role; permissions may be supplied inline or added afterwards")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Role created"),
        @ApiResponse(responseCode = "400", description = "Invalid input or unknown entity/field"),
        @ApiResponse(responseCode = "409", description = "Role name already exists")
    })
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleDto dto) {
        return new ResponseEntity<>(roleService.createRole(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List roles", description = "Retrieve all roles with pagination")
    @Parameter(name = "page", description = "Zero-indexed page number", in = ParameterIn.QUERY, example = "0")
    @Parameter(name = "size", description = "Page size", in = ParameterIn.QUERY, example = "20")
    @Parameter(name = "sort", description = "Sort field and direction (e.g. name,asc)", in = ParameterIn.QUERY, example = "id,asc")
    public ResponseEntity<Page<RoleDto>> getAllRoles(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(roleService.getAllRoles(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieve a role with its permissions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role found"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Update name and description (permissions are ignored here)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role updated"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "409", description = "Name already exists, or the role is a system role")
    })
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto dto) {
        return ResponseEntity.ok(roleService.updateRole(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role",
        description = "Deletes a role and every assignment of it; users holding it lose those grants")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Role deleted"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "409", description = "System roles cannot be deleted")
    })
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "List a role's permissions")
    public ResponseEntity<List<PermissionDto>> getPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getPermissions(id));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Replace a role's permissions",
        description = "Makes the role's permissions exactly this list. Each item is {entity, field, operation}; "
            + "field defaults to '*'. Unknown entities or fields are rejected with 400.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permissions replaced"),
        @ApiResponse(responseCode = "400", description = "Unknown entity or field"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "409", description = "System role permissions cannot be changed")
    })
    public ResponseEntity<List<PermissionDto>> replacePermissions(@PathVariable Long id,
                                                                  @RequestBody List<PermissionDto> permissions) {
        return ResponseEntity.ok(roleService.replacePermissions(id, permissions));
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "Add one permission to a role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Permission added"),
        @ApiResponse(responseCode = "400", description = "Unknown entity or field"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "409", description = "Permission already present, or system role")
    })
    public ResponseEntity<PermissionDto> addPermission(@PathVariable Long id,
                                                       @Valid @RequestBody PermissionDto permission) {
        return new ResponseEntity<>(roleService.addPermission(id, permission), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @Operation(summary = "Remove one permission from a role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Permission removed"),
        @ApiResponse(responseCode = "404", description = "Role or permission not found"),
        @ApiResponse(responseCode = "409", description = "System role permissions cannot be changed")
    })
    public ResponseEntity<Void> removePermission(@PathVariable Long id, @PathVariable Long permissionId) {
        roleService.removePermission(id, permissionId);
        return ResponseEntity.noContent().build();
    }
}
