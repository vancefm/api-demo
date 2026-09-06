package com.demo.feature.security.rbac;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * The RBAC feature's own resources as secured entities, so that managing roles
 * and grants is itself governed by permissions.
 *
 * <ul>
 *   <li>{@code Role} is not departmental: only a global grant can manage roles.</li>
 *   <li>{@code RoleAssignment} is scoped to the department it grants, so a
 *       department-scoped administrator can hand out roles within their own
 *       department; a global grant (department null) needs a global permission.</li>
 * </ul>
 */
@Configuration
public class RbacSecuredEntities {

    public static final String ROLE = "Role";
    public static final String ROLE_ASSIGNMENT = "RoleAssignment";

    @Bean
    public SecuredEntity<RoleDto> securedRole() {
        return SecuredEntity.global(ROLE, RoleDto.class);
    }

    @Bean
    public SecuredEntity<RoleAssignmentDto> securedRoleAssignment() {
        return new SecuredEntity<>(ROLE_ASSIGNMENT, RoleAssignmentDto.class,
            dto -> dto.getDepartmentId() == null ? Set.of() : Set.of(dto.getDepartmentId()));
    }
}
