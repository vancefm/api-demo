package com.demo.feature.security.rbac;

import org.springframework.stereotype.Component;

/**
 * Maps {@link RoleAssignment} to its DTO. Inbound mapping needs entity lookups
 * (user, role, department), so {@code RoleAssignmentService} does it.
 */
@Component
public class RoleAssignmentMapper {

    public RoleAssignmentDto toDto(RoleAssignment entity) {
        if (entity == null) {
            return null;
        }

        return RoleAssignmentDto.builder()
            .id(entity.getId())
            .userId(entity.getUser().getId())
            .roleId(entity.getRole().getId())
            .roleName(entity.getRole().getName())
            .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
            .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : null)
            .build();
    }
}
