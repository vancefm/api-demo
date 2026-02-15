package com.demo.shared.security;

import com.demo.domain.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for filtering DTO fields based on user permissions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FieldPermissionFilterService {
    
    private final AuthorizationService authorizationService;
    private final FieldPermissionsConfig fieldPermissionsConfig;
    private final ObjectMapper objectMapper;
    
    /**
     * Filter fields from a DTO based on user's read permissions.
     * Returns a map representation with only allowed fields.
     */
    public Map<String, Object> filterReadableFields(User user, Object dto, String resourceType) {
        // Convert DTO to map
        @SuppressWarnings("unchecked")
        Map<String, Object> dtoMap = objectMapper.convertValue(dto, Map.class);
        
        // Remove fields the user cannot read
        dtoMap.entrySet().removeIf(entry -> {
            String fieldName = entry.getKey();
            
            // Check if field is hidden
            if (fieldPermissionsConfig.isHidden(resourceType, fieldName)) {
                return true;
            }
            
            // Check user's read permission
            return !authorizationService.canReadField(user, resourceType, fieldName);
        });
        
        return dtoMap;
    }
    
    /**
     * Validate that a user can write to all fields in the provided DTO.
     * Throws exception if any field is not writable.
     */
    public void validateWritableFields(User user, Map<String, Object> fieldsToWrite, 
                                      String resourceType, boolean isUpdate) {
        for (String fieldName : fieldsToWrite.keySet()) {
            // Check if field is immutable and this is an update
            if (isUpdate && fieldPermissionsConfig.isImmutable(resourceType, fieldName)) {
                throw new SecurityException("Field '" + fieldName + "' is immutable and cannot be modified");
            }
            
            // Check if field is read-only
            if (fieldPermissionsConfig.isReadOnly(resourceType, fieldName)) {
                throw new SecurityException("Field '" + fieldName + "' is read-only and cannot be modified");
            }
            
            // Check user's write permission
            if (!authorizationService.canWriteField(user, resourceType, fieldName)) {
                throw new SecurityException("You do not have permission to modify field: " + fieldName);
            }
        }
    }
}
