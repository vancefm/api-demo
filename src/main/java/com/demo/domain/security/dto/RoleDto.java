package com.demo.domain.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating or updating a role.
 */
@Schema(description = "Role Data Transfer Object")
public class RoleDto {
    
    @Schema(description = "Role ID", example = "1")
    private Long id;
    
    @NotBlank(message = "Role name is required")
    @Schema(description = "Role name", example = "DEVELOPER")
    private String name;
    
    @NotNull(message = "Hierarchy level is required")
    @Schema(description = "Hierarchy level (higher = more privileges)", example = "30")
    private Integer hierarchyLevel;
    
    @Schema(description = "Role description", example = "Developer role with code access")
    private String description;
    
    // Constructors
    public RoleDto() {
    }
    
    public RoleDto(Long id, String name, Integer hierarchyLevel, String description) {
        this.id = id;
        this.name = name;
        this.hierarchyLevel = hierarchyLevel;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }
    
    public void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
