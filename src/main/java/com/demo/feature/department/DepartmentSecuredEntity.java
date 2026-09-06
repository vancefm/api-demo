package com.demo.feature.department;

import com.demo.feature.security.rbac.SecuredEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Registers {@code Department} with the RBAC layer. A department is its own
 * scope: a grant scoped to department D applies to department D itself. A
 * department that has no id yet (being created) has no scope, so creating
 * departments needs a global grant.
 */
@Configuration
public class DepartmentSecuredEntity {

    public static final String NAME = "Department";

    @Bean
    public SecuredEntity<DepartmentDto> securedDepartment() {
        return new SecuredEntity<>(NAME, DepartmentDto.class,
            dto -> dto.getId() == null ? Set.of() : Set.of(dto.getId()));
    }
}
