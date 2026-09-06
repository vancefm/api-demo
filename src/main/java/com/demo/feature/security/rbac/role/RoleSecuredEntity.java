package com.demo.feature.security.rbac.role;

import com.demo.feature.security.rbac.access.SecuredEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@code Role} with the RBAC layer so that managing roles is itself
 * governed by permissions. Roles are not departmental: only a global grant can
 * manage them, because a role is reusable across departments and letting a
 * department administrator edit one would leak changes into departments they
 * do not control.
 */
@Configuration
public class RoleSecuredEntity {

    public static final String NAME = "Role";

    @Bean
    public SecuredEntity<RoleDto> securedRole() {
        return SecuredEntity.global(NAME, RoleDto.class);
    }
}
