package com.demo.feature.security.rbac.assignment;
import com.demo.feature.security.rbac.role.Role;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code NON_NULL}: a global grant simply has no {@code departmentId} key, and
 * fields the caller may not read are omitted.
 */
@Schema(description = "A Department:Role grant held by a user. Omit departmentId for a global grant.")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAssignmentDto {

    @Schema(description = "Assignment ID", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "User holding the grant", example = "7", accessMode = Schema.AccessMode.READ_ONLY)
    private Long userId;

    @NotNull(message = "Role ID is required")
    @Schema(description = "Role granted", example = "2")
    private Long roleId;

    @Schema(description = "Role name", example = "Department User", accessMode = Schema.AccessMode.READ_ONLY)
    private String roleName;

    @Schema(description = "Department the grant is scoped to; null means global", example = "1")
    private Long departmentId;

    @Schema(description = "Department name; null means global", example = "IT", accessMode = Schema.AccessMode.READ_ONLY)
    private String departmentName;
}
