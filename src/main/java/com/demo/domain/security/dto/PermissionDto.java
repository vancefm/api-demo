package com.demo.domain.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating or updating a permission.
 */
@Schema(description = "Permission Data Transfer Object")
public class PermissionDto {
    
    @Schema(description = "Permission ID", example = "1")
    private Long id;
    
    @NotBlank(message = "Resource type is required")
    @Schema(description = "Resource type", example = "ComputerSystem")
    private String resourceType;
    
    @NotBlank(message = "Operation is required")
    @Schema(description = "Operation", example = "READ")
    private String operation;
    
    @NotBlank(message = "Scope is required")
    @Schema(description = "Scope", example = "ALL")
    private String scope;
    
    @Schema(description = "Field permissions as JSON", 
            example = "{\"systemUser\":\"READ\",\"department\":\"READ\"}")
    private String fieldPermissions;
    
    // Constructors
    public PermissionDto() {
    }
    
    public PermissionDto(Long id, String resourceType, String operation, 
                        String scope, String fieldPermissions) {
        this.id = id;
        this.resourceType = resourceType;
        this.operation = operation;
        this.scope = scope;
        this.fieldPermissions = fieldPermissions;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getFieldPermissions() {
        return fieldPermissions;
    }
    
    public void setFieldPermissions(String fieldPermissions) {
        this.fieldPermissions = fieldPermissions;
    }
}
