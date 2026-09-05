package com.demo.feature.user;

import com.demo.feature.department.DepartmentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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
    
    @Schema(description = "IDs of departments this user belongs to (write side; optional)", example = "[1, 3]")
    private List<Long> departmentIds;

    @Schema(description = "Departments this user belongs to (populated on read)", accessMode = Schema.AccessMode.READ_ONLY)
    private List<DepartmentDto> departments;

    @Schema(description = "Manager user ID", example = "2")
    private Long managerId;
}
