package com.demo.feature.security.rbac;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * {@code NON_NULL}: fields the caller may not read are nulled by the RBAC layer
 * and therefore omitted from the JSON.
 */
@Schema(description = "Role Data Transfer Object")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {

    @Schema(description = "Role ID", example = "1")
    private Long id;

    @NotBlank(message = "Role name is required")
    @Schema(description = "Role name", example = "Department User")
    private String name;

    @Schema(description = "Role description", example = "Reads and edits names of users in its department")
    private String description;

    @Schema(description = "Seeded at startup; cannot be renamed, deleted or re-permissioned",
        accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean system;

    @Valid
    @Schema(description = "Permissions. Accepted on create as a convenience; afterwards managed via "
        + "/api/v1/roles/{id}/permissions and ignored on PUT /api/v1/roles/{id}")
    private List<PermissionDto> permissions;
}
