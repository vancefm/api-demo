package com.demo.feature.department;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code NON_NULL}: fields the caller may not read are nulled by the RBAC layer
 * and therefore omitted from the JSON.
 */
@Schema(description = "Department Data Transfer Object")
@JsonInclude(JsonInclude.Include.NON_NULL)
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
