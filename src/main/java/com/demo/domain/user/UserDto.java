package com.demo.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating or updating a user.
 */
@Schema(description = "User Data Transfer Object")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    
    @Schema(description = "Role name (read-only)", example = "MY_APP_USER")
    private String roleName;

    @Schema(description = "Manager user ID", example = "2")
    private Long managerId;
}
