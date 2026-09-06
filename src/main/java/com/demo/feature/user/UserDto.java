package com.demo.feature.user;

import com.demo.feature.department.DepartmentDto;
import com.demo.feature.security.rbac.RoleAssignmentDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for creating or updating a user.
 *
 * <p>{@code NON_NULL}: fields the caller may not read are nulled by the RBAC
 * layer and therefore omitted from the JSON (as are genuinely absent optional
 * values such as an unset {@code managerId}).
 */
@Schema(description = "User Data Transfer Object")
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    @Schema(description = "Given name", example = "John")
    private String firstName;

    @Schema(description = "Family name", example = "Doe")
    private String lastName;

    @Schema(description = "IDs of departments this user belongs to (write side; optional)", example = "[1, 3]")
    private List<Long> departmentIds;

    @Schema(description = "Departments this user belongs to (populated on read)", accessMode = Schema.AccessMode.READ_ONLY)
    private List<DepartmentDto> departments;

    @Schema(description = "Manager user ID", example = "2")
    private Long managerId;

    @Schema(description = "Department:Role grants held by this user (populated on read; managed via "
        + "/api/v1/users/{id}/role-assignments)", accessMode = Schema.AccessMode.READ_ONLY)
    private List<RoleAssignmentDto> roleAssignments;
}
