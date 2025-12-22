package com.demo.application.security;

import com.demo.domain.security.dto.PermissionDto;
import com.demo.domain.security.dto.RoleDto;
import com.demo.domain.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for role, permission, and user management.
 * These endpoints should only be accessible by SUPER_ADMIN users.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Role Management", description = "Admin APIs for managing roles, permissions, and users")
public class RoleManagementController {
    
    private final RoleManagementService roleManagementService;
    
    public RoleManagementController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }
    
    // ===== Role Endpoints =====
    
    @PostMapping("/roles")
    @Operation(summary = "Create a new role", description = "Create a new role in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Role created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Role already exists")
    })
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleDto dto) {
        RoleDto created = roleManagementService.createRole(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
    
    @GetMapping("/roles")
    @Operation(summary = "Get all roles", description = "Retrieve all roles in the system")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<RoleDto> roles = roleManagementService.getAllRoles();
        return ResponseEntity.ok(roles);
    }
    
    @GetMapping("/roles/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieve a specific role by its ID")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        RoleDto role = roleManagementService.getRoleById(id);
        return ResponseEntity.ok(role);
    }
    
    @PutMapping("/roles/{id}")
    @Operation(summary = "Update role", description = "Update an existing role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role updated successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "409", description = "Role name already exists")
    })
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto dto) {
        RoleDto updated = roleManagementService.updateRole(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/roles/{id}")
    @Operation(summary = "Delete role", description = "Delete a role from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Role deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleManagementService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
    
    // ===== Permission Endpoints =====
    
    @PostMapping("/permissions")
    @Operation(summary = "Create a new permission", description = "Create a new permission in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Permission created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<PermissionDto> createPermission(@Valid @RequestBody PermissionDto dto) {
        PermissionDto created = roleManagementService.createPermission(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
    
    @GetMapping("/permissions")
    @Operation(summary = "Get all permissions", description = "Retrieve all permissions in the system")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        List<PermissionDto> permissions = roleManagementService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }
    
    @GetMapping("/permissions/{id}")
    @Operation(summary = "Get permission by ID", description = "Retrieve a specific permission by its ID")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable Long id) {
        PermissionDto permission = roleManagementService.getPermissionById(id);
        return ResponseEntity.ok(permission);
    }
    
    @PutMapping("/permissions/{id}")
    @Operation(summary = "Update permission", description = "Update an existing permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission updated successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    public ResponseEntity<PermissionDto> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionDto dto) {
        PermissionDto updated = roleManagementService.updatePermission(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/permissions/{id}")
    @Operation(summary = "Delete permission", description = "Delete a permission from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Permission deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        roleManagementService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
    
    // ===== Role-Permission Assignment Endpoints =====
    
    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Assign permission to role", description = "Grant a permission to a role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission assigned successfully"),
        @ApiResponse(responseCode = "404", description = "Role or permission not found"),
        @ApiResponse(responseCode = "409", description = "Role already has this permission")
    })
    public ResponseEntity<Void> assignPermissionToRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        roleManagementService.assignPermissionToRole(roleId, permissionId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Revoke permission from role", description = "Revoke a permission from a role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Permission revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Role or permission not found")
    })
    public ResponseEntity<Void> revokePermissionFromRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        roleManagementService.revokePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Get role permissions", description = "Get all permissions for a specific role")
    public ResponseEntity<List<PermissionDto>> getPermissionsForRole(@PathVariable Long roleId) {
        List<PermissionDto> permissions = roleManagementService.getPermissionsForRole(roleId);
        return ResponseEntity.ok(permissions);
    }
    
    // ===== User Endpoints =====
    
    @PostMapping("/users")
    @Operation(summary = "Create a new user", description = "Create a new user in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto dto) {
        UserDto created = roleManagementService.createUser(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
    
    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Retrieve all users in the system")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = roleManagementService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = roleManagementService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/users/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto dto) {
        UserDto updated = roleManagementService.updateUser(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user", description = "Delete a user from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        roleManagementService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    // ===== Cache Management =====
    
    @PostMapping("/cache/reload")
    @Operation(summary = "Reload permissions cache", 
               description = "Reload the role permissions cache to pick up database changes")
    @ApiResponse(responseCode = "200", description = "Cache reloaded successfully")
    public ResponseEntity<Map<String, String>> reloadCache() {
        roleManagementService.reloadPermissionsCache();
        return ResponseEntity.ok(Map.of("message", "Permissions cache reloaded successfully"));
    }
}
