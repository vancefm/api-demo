package com.demo.domain.department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating, updating and returning departments.
 */
@Schema(description = "Department Data Transfer Object")
public record DepartmentDto(
        @Schema(description = "Department id", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @NotBlank(message = "Department name is required")
        @Schema(description = "Department name", example = "IT")
        String name) {
}
