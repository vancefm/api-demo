package com.demo.feature.department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Department Data Transfer Object")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {

    @Schema(description = "Department ID", example = "1")
    private Long id;

    @NotBlank(message = "Department name is required")
    @Schema(description = "Department name", example = "IT")
    private String name;

    @Schema(description = "Department description", example = "Information Technology")
    private String description;
}
