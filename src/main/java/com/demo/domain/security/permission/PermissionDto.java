package com.demo.domain.security.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating or updating a permission.
 */
@Schema(description = "Permission Data Transfer Object")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
            example = "{\"userId\":\"READ\",\"department\":\"READ\"}")
    private String fieldPermissions;
}
