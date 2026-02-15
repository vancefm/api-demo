package com.demo.shared.security;

import com.demo.domain.security.role.Role;
import com.demo.application.security.auth.PermissionRepository;
import com.demo.application.security.auth.RolePermissionRepository;
import com.demo.application.security.auth.RoleRepository;
import com.demo.domain.security.permission.Permission;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that loads and caches role permissions.
 * Provides methods to check permissions and reload cache without redeployment.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RolePermissionService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ObjectMapper objectMapper;
    
    // Cache: roleName -> list of permissions
    private final Map<String, List<Permission>> rolePermissionsCache = new ConcurrentHashMap<>();
    
    /**
     * Get all permissions for a role.
     * Uses cache if available, otherwise loads from database.
     */
    public List<Permission> getPermissionsForRole(String roleName) {
        if (rolePermissionsCache.containsKey(roleName)) {
            return rolePermissionsCache.get(roleName);
        }
        
        return loadPermissionsForRole(roleName);
    }
    
    /**
     * Load permissions for a role from database and cache them.
     */
    private List<Permission> loadPermissionsForRole(String roleName) {
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            log.warn("Role not found: {}", roleName);
            return Collections.emptyList();
        }
        
        Role role = roleOpt.get();
        List<Permission> permissions = rolePermissionRepository.findPermissionsByRole(role);
        rolePermissionsCache.put(roleName, permissions);
        
        log.debug("Loaded {} permissions for role: {}", permissions.size(), roleName);
        return permissions;
    }
    
    /**
     * Reload all role permissions from database.
     * Call this after modifying roles or permissions via admin API.
     */
    public void reloadCache() {
        log.info("Reloading role permissions cache");
        rolePermissionsCache.clear();
        
        // Pre-load all roles
        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            loadPermissionsForRole(role.getName());
        }
        
        log.info("Role permissions cache reloaded successfully");
    }
    
    /**
     * Check if a role has permission for a specific operation on a resource type.
     */
    public boolean hasPermission(String roleName, String resourceType, String operation, String scope) {
        List<Permission> permissions = getPermissionsForRole(roleName);
        
        return permissions.stream()
            .anyMatch(p -> 
                p.getResourceType().equals(resourceType) &&
                p.getOperation().equals(operation) &&
                (p.getScope().equals("ALL") || p.getScope().equals(scope))
            );
    }
    
    /**
     * Get field permissions for a role, resource type, and operation.
     * Returns a map of field names to permission levels (READ, WRITE, HIDDEN).
     */
    public Map<String, String> getFieldPermissions(String roleName, String resourceType, String operation) {
        List<Permission> permissions = getPermissionsForRole(roleName);
        
        for (Permission permission : permissions) {
            if (permission.getResourceType().equals(resourceType) &&
                permission.getOperation().equals(operation)) {
                
                String fieldPermsJson = permission.getFieldPermissions();
                if (fieldPermsJson != null && !fieldPermsJson.isEmpty()) {
                    try {
                        return objectMapper.readValue(fieldPermsJson, new TypeReference<Map<String, String>>() {});
                    } catch (Exception e) {
                        log.error("Error parsing field permissions JSON: {}", e.getMessage());
                    }
                }
            }
        }
        
        return Collections.emptyMap();
    }
}
