package com.demo.shared.security;

import com.demo.domain.security.role.Role;
import com.demo.application.security.auth.PermissionRepository;
import com.demo.application.security.auth.RolePermissionRepository;
import com.demo.application.security.auth.RoleRepository;
import com.demo.domain.security.permission.Permission;
import com.demo.domain.security.rolepermission.RolePermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service that initializes default roles and permissions on application startup.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleInitializationService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionService rolePermissionService;
    private final ObjectMapper objectMapper;
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDefaultRoles() {
        log.info("Initializing default roles and permissions");
        
        // Create default roles if they don't exist
        Role superAdmin = createRoleIfNotExists("MY_APP_SUPERADMIN", 
            "Full system access with all permissions");
        Role admin = createRoleIfNotExists("MY_APP_ADMIN", 
            "Department-scoped access with limited field modifications");
        Role user = createRoleIfNotExists("MY_APP_USER", 
            "Access to own resources only, cannot modify sensitive fields");
        
        // Create ComputerSystem permissions for MY_APP_SUPERADMIN
        createComputerSystemPermissionsForSuperAdmin(superAdmin);
        
        // Create ComputerSystem permissions for MY_APP_ADMIN
        createComputerSystemPermissionsForAdmin(admin);
        
        // Create ComputerSystem permissions for MY_APP_USER
        createComputerSystemPermissionsForUser(user);
        
        // Reload cache
        rolePermissionService.reloadCache();
        
        log.info("Default roles and permissions initialized successfully");
    }
    
    private Role createRoleIfNotExists(String name, String description) {
        Optional<Role> existingRole = roleRepository.findByName(name);
        if (existingRole.isPresent()) {
            log.debug("Role already exists: {}", name);
            return existingRole.get();
        }
        
        Role role = Role.builder()
            .name(name)
            .description(description)
            .build();
        
        Role saved = roleRepository.save(role);
        log.info("Created role: {}", name);
        return saved;
    }
    
    private void createComputerSystemPermissionsForSuperAdmin(Role role) {
        // SUPER_ADMIN: Full access to all fields, ALL scope
        Map<String, String> fieldPerms = new HashMap<>();
        // All fields are writable for SUPER_ADMIN
        
        createPermissionAndAssign(role, "ComputerSystem", "READ", "ALL", 
            toJson(fieldPerms));
        createPermissionAndAssign(role, "ComputerSystem", "WRITE", "ALL", 
            toJson(fieldPerms));
        createPermissionAndAssign(role, "ComputerSystem", "DELETE", "ALL", 
            toJson(fieldPerms));
    }
    
    private void createComputerSystemPermissionsForAdmin(Role role) {
        // ADMIN: Department-scoped access, limited field modifications
        Map<String, String> readFieldPerms = new HashMap<>();
        // Can read all fields
        
        Map<String, String> writeFieldPerms = new HashMap<>();
        // Cannot modify certain sensitive fields
        writeFieldPerms.put("userId", "READ"); // Cannot change user
        writeFieldPerms.put("department", "READ"); // Cannot change department
        writeFieldPerms.put("networkName", "READ"); // Cannot change network
        
        createPermissionAndAssign(role, "ComputerSystem", "READ", "DEPARTMENT", 
            toJson(readFieldPerms));
        createPermissionAndAssign(role, "ComputerSystem", "WRITE", "DEPARTMENT", 
            toJson(writeFieldPerms));
        createPermissionAndAssign(role, "ComputerSystem", "DELETE", "DEPARTMENT", 
            toJson(new HashMap<>()));
    }
    
    private void createComputerSystemPermissionsForUser(Role role) {
        // USER: Own resources only, cannot modify sensitive fields
        Map<String, String> readFieldPerms = new HashMap<>();
        // Can read all fields of own resources
        
        Map<String, String> writeFieldPerms = new HashMap<>();
        // Cannot modify these sensitive fields
        writeFieldPerms.put("userId", "READ");
        writeFieldPerms.put("department", "READ");
        writeFieldPerms.put("networkName", "READ");
        writeFieldPerms.put("macAddress", "READ");
        writeFieldPerms.put("ipAddress", "READ");
        
        createPermissionAndAssign(role, "ComputerSystem", "READ", "OWN", 
            toJson(readFieldPerms));
        createPermissionAndAssign(role, "ComputerSystem", "WRITE", "OWN", 
            toJson(writeFieldPerms));
        // USER cannot delete
    }
    
    private void createPermissionAndAssign(Role role, String resourceType, 
                                          String operation, String scope, String fieldPermissions) {
        // Check if permission already exists
        var existingPerms = permissionRepository.findByResourceTypeAndOperationAndScope(
            resourceType, operation, scope);
        
        Permission permission;
        if (!existingPerms.isEmpty()) {
            permission = existingPerms.get(0);
            log.debug("Permission already exists: {} {} {}", resourceType, operation, scope);
        } else {
            permission = Permission.builder()
                .resourceType(resourceType)
                .operation(operation)
                .scope(scope)
                .fieldPermissions(fieldPermissions)
                .build();
            permission = permissionRepository.save(permission);
            log.debug("Created permission: {} {} {}", resourceType, operation, scope);
        }
        
        // Check if role-permission mapping exists
        if (!rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
            RolePermission rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .build();
            rolePermissionRepository.save(rolePermission);
            log.debug("Assigned permission to role: {}", role.getName());
        }
    }
    
    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Error converting map to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
