package com.demo.feature.computersystem;

import com.demo.feature.security.rbac.SecuredEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@code ComputerSystem} with the RBAC layer. Field names in
 * permissions are {@link ComputerSystemDto} property names; a system's
 * department scope is its {@code departmentIds}.
 */
@Configuration
public class ComputerSystemSecuredEntity {

    public static final String NAME = "ComputerSystem";

    @Bean
    public SecuredEntity<ComputerSystemDto> securedComputerSystem() {
        return SecuredEntity.departmental(NAME, ComputerSystemDto.class, ComputerSystemDto::getDepartmentIds);
    }
}
