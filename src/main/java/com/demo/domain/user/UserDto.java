package com.demo.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating or updating a user.
 */
@Schema(description = "User Data Transfer Object")
public class UserDto {
    
    @Schema(description = "User ID", example = "1")
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "john.doe")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Schema(description = "Email", example = "john.doe@example.com")
    private String email;
    
    @NotBlank(message = "Department is required")
    @Schema(description = "Department", example = "IT")
    private String department;
    
    @NotNull(message = "Role ID is required")
    @Schema(description = "Role ID", example = "1")
    private Long roleId;
    
    @Schema(description = "Role name (read-only)", example = "USER")
    private String roleName;
    
    // Constructors
    public UserDto() {
    }
    
    public UserDto(Long id, String username, String email, String department, 
                  Long roleId, String roleName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.department = department;
        this.roleId = roleId;
        this.roleName = roleName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public Long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
