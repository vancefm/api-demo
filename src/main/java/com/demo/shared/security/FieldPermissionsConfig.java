package com.demo.shared.security;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Configuration class defining immutable and read-only fields for different resource types.
 * This defines which fields cannot be modified after creation or are read-only.
 */
@Component
public class FieldPermissionsConfig {
    
    // Maps resource type to immutable fields (cannot be changed after creation)
    private final Map<String, Set<String>> immutableFields = new HashMap<>();
    
    // Maps resource type to read-only fields (can be read but not modified)
    private final Map<String, Set<String>> readOnlyFields = new HashMap<>();
    
    // Maps resource type to hidden fields (completely hidden from response)
    private final Map<String, Set<String>> hiddenFields = new HashMap<>();
    
    public FieldPermissionsConfig() {
        initializeComputerSystemFields();
    }
    
    private void initializeComputerSystemFields() {
        // Immutable fields for ComputerSystem (cannot change after creation)
        immutableFields.put("ComputerSystem", Set.of("id", "createdAt", "createdById"));
        
        // Read-only fields for ComputerSystem (can read but not write)
        readOnlyFields.put("ComputerSystem", Set.of("updatedAt", "updatedById"));
        
        // Hidden fields are role-dependent and handled by AuthorizationService
        hiddenFields.put("ComputerSystem", new HashSet<>());
    }
    
    /**
     * Check if a field is immutable (cannot be changed after creation).
     */
    public boolean isImmutable(String resourceType, String fieldName) {
        Set<String> fields = immutableFields.get(resourceType);
        return fields != null && fields.contains(fieldName);
    }
    
    /**
     * Check if a field is read-only (can be read but not modified).
     */
    public boolean isReadOnly(String resourceType, String fieldName) {
        Set<String> fields = readOnlyFields.get(resourceType);
        return fields != null && fields.contains(fieldName);
    }
    
    /**
     * Check if a field is hidden (completely hidden from response).
     */
    public boolean isHidden(String resourceType, String fieldName) {
        Set<String> fields = hiddenFields.get(resourceType);
        return fields != null && fields.contains(fieldName);
    }
    
    /**
     * Get all immutable fields for a resource type.
     */
    public Set<String> getImmutableFields(String resourceType) {
        return new HashSet<>(immutableFields.getOrDefault(resourceType, Set.of()));
    }
    
    /**
     * Get all read-only fields for a resource type.
     */
    public Set<String> getReadOnlyFields(String resourceType) {
        return new HashSet<>(readOnlyFields.getOrDefault(resourceType, Set.of()));
    }
}
