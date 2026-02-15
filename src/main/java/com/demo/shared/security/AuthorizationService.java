package com.demo.shared.security;

import com.demo.domain.computersystem.ComputerSystem;
import com.demo.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for checking object-level and field-level authorization.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {
    
    private final RolePermissionService rolePermissionService;
    
    /**
     * Check if a user can perform an operation on a resource type.
     */
    public boolean canAccess(User user, String resourceType, String operation) {
        String roleName = user.getRole().getName();
        
        // Check for ALL scope first
        if (rolePermissionService.hasPermission(roleName, resourceType, operation, "ALL")) {
            return true;
        }
        
        // DEPARTMENT and OWN scopes require additional context
        return false;
    }
    
    /**
     * Check if a user can access a specific ComputerSystem.
     * Takes into account permission scope (OWN, DEPARTMENT, ALL).
     */
    public boolean canAccessComputerSystem(User user, ComputerSystem computerSystem, String operation) {
        String roleName = user.getRole().getName();
        
        // Check ALL scope
        if (rolePermissionService.hasPermission(roleName, "ComputerSystem", operation, "ALL")) {
            return true;
        }
        
        // Check DEPARTMENT scope
        if (rolePermissionService.hasPermission(roleName, "ComputerSystem", operation, "DEPARTMENT")) {
            if (computerSystem.getDepartment().equals(user.getDepartment())) {
                return true;
            }
        }
        
        // Check OWN scope
        if (rolePermissionService.hasPermission(roleName, "ComputerSystem", operation, "OWN")) {
            if (computerSystem.getCreatedBy() != null && 
                computerSystem.getCreatedBy().getId().equals(user.getId())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a user can read a specific field.
     */
    public boolean canReadField(User user, String resourceType, String fieldName) {
        String roleName = user.getRole().getName();
        Map<String, String> fieldPermissions = 
            rolePermissionService.getFieldPermissions(roleName, resourceType, "READ");
        
        String permission = fieldPermissions.get(fieldName);
        
        // If no specific permission is set, default to READ
        if (permission == null) {
            return true;
        }
        
        return !permission.equals("HIDDEN");
    }
    
    /**
     * Check if a user can write to a specific field.
     */
    public boolean canWriteField(User user, String resourceType, String fieldName) {
        String roleName = user.getRole().getName();
        Map<String, String> fieldPermissions = 
            rolePermissionService.getFieldPermissions(roleName, resourceType, "WRITE");
        
        String permission = fieldPermissions.get(fieldName);
        
        // If no specific permission is set, default to WRITE allowed
        if (permission == null) {
            return true;
        }
        
        return permission.equals("WRITE");
    }
}
