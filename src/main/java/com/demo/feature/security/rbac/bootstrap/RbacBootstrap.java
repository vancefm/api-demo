package com.demo.feature.security.rbac.bootstrap;
import com.demo.feature.security.rbac.role.Permission;
import com.demo.feature.security.rbac.assignment.RoleAssignmentRepository;
import com.demo.feature.security.rbac.assignment.RoleAssignment;
import com.demo.feature.security.rbac.role.Role;
import com.demo.feature.security.rbac.role.RoleRepository;
import com.demo.feature.security.rbac.role.Operation;

import com.demo.feature.user.User;
import com.demo.feature.user.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes a fresh database usable: seeds the locked {@code SuperAdmin} role
 * ({@code * / * / every operation}) and gives the configured bootstrap user a
 * global grant of it. Idempotent — safe to run on every start.
 *
 * <p>This is the only grant that is not created through the API. Everything
 * else (departments, further roles, other users' grants) is expected to be set
 * up by that administrator — see the README's "First-time use".
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RbacBootstrap {

    private final RbacProperties properties;
    private final RoleRepository roleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final UserManagementService userManagementService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        RbacProperties.Bootstrap bootstrap = properties.getBootstrap();
        if (!bootstrap.isEnabled()) {
            log.warn("RBAC bootstrap disabled (app.rbac.bootstrap.enabled=false): no SuperAdmin will be seeded");
            return;
        }

        Role superAdmin = ensureSuperAdminRole(bootstrap.getSuperAdminRole());
        User admin = userManagementService.findOrProvision(bootstrap.getUsername(), bootstrap.getEmail(), null, null);

        if (roleAssignmentRepository.existsByUserIdAndRoleIdAndDepartmentIsNull(admin.getId(), superAdmin.getId())) {
            log.info("RBAC bootstrap: user '{}' already holds '{}' globally", admin.getUsername(), superAdmin.getName());
            return;
        }

        RoleAssignment grant = roleAssignmentRepository.save(RoleAssignment.builder()
            .user(admin)
            .role(superAdmin)
            .department(null)
            .build());
        admin.getRoleAssignments().add(grant);
        log.info("RBAC bootstrap: granted '{}' globally to user '{}'", superAdmin.getName(), admin.getUsername());
    }

    private Role ensureSuperAdminRole(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder()
                .name(name)
                .description("Full access to every entity and every field")
                .system(true)
                .build();
            for (Operation operation : Operation.values()) {
                role.addPermission(Permission.ANY, Permission.ANY, operation);
            }
            Role saved = roleRepository.save(role);
            log.info("RBAC bootstrap: created system role '{}'", saved.getName());
            return saved;
        });
    }
}
