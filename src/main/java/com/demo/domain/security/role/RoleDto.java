package com.demo.domain.security.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating or updating a role.
 */
@Schema(description = "Role Data Transfer Object")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    
    @Schema(description = "Role ID", example = "1")
    private Long id;
    
    @NotBlank(message = "Role name is required")
    @Schema(description = "Role name", example = "DEVELOPER")
    private String name;
    
    @Schema(description = "Role description", example = "Developer role with code access")
    private String description;
}
