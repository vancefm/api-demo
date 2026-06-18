package com.demo.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

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
    
    @NotEmpty(message = "At least one department is required")
    @Schema(description = "Ids of the departments this user belongs to", example = "[1, 2]")
    private Set<Long> departmentIds;

    @Schema(description = "Names of the departments this user belongs to (read-only)", example = "[\"IT\", \"Finance\"]", accessMode = Schema.AccessMode.READ_ONLY)
    private Set<String> departmentNames;

    @NotEmpty(message = "At least one role is required")
    @Schema(description = "Ids of the roles assigned to this user", example = "[1]")
    private Set<Long> roleIds;

    @Schema(description = "Names of the roles assigned to this user (read-only)", example = "[\"MY_APP_USER\"]", accessMode = Schema.AccessMode.READ_ONLY)
    private Set<String> roleNames;

    @Schema(description = "Manager user ID", example = "2")
    private Long managerId;
}
