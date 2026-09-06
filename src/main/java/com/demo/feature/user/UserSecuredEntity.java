package com.demo.feature.user;

import com.demo.feature.security.rbac.SecuredEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@code User} with the RBAC layer. Field names in permissions are
 * {@link UserDto} property names; a user's department scope is its
 * {@code departmentIds}.
 */
@Configuration
public class UserSecuredEntity {

    public static final String NAME = "User";

    @Bean
    public SecuredEntity<UserDto> securedUser() {
        return SecuredEntity.departmental(NAME, UserDto.class, UserDto::getDepartmentIds);
    }
}
