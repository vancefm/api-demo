package com.demo.feature.security.rbac.assignment;

import com.demo.feature.security.rbac.access.SecuredEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Registers {@code RoleAssignment} with the RBAC layer. A grant is scoped to
 * the department it hands out, so a department-scoped administrator can grant
 * and revoke roles within their own department; a global grant (department
 * null) has no scope and therefore needs a global permission.
 */
@Configuration
public class RoleAssignmentSecuredEntity {

    public static final String NAME = "RoleAssignment";

    @Bean
    public SecuredEntity<RoleAssignmentDto> securedRoleAssignment() {
        return new SecuredEntity<>(NAME, RoleAssignmentDto.class,
            dto -> dto.getDepartmentId() == null ? Set.of() : Set.of(dto.getDepartmentId()));
    }
}
