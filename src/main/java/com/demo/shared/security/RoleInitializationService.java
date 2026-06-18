package com.demo.shared.security;

import com.demo.domain.security.role.Role;
import com.demo.application.security.auth.PermissionRepository;
import com.demo.application.security.auth.RolePermissionRepository;
import com.demo.application.security.auth.RoleRepository;
import com.demo.application.user.UserRepository;
import com.demo.domain.security.permission.Permission;
import com.demo.domain.security.rolepermission.RolePermission;
import com.demo.domain.user.User;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Seeds the default roles + permissions and a bootstrap database superadmin on startup.
 *
 * <p>Field policy semantics (see {@link GrantService}): an empty {@code fieldPermissions}
 * map grants the full field set; a non-empty map is a whitelist — for READ, listed fields
 * are readable; for WRITE, only fields marked {@code WRITE} are writable.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleInitializationService {

    public static final String SUPERADMIN = "MY_APP_SUPERADMIN";
    public static final String ADMIN = "MY_APP_ADMIN";
    public static final String USER = "MY_APP_USER";

    private static final String COMPUTER_SYSTEM = "ComputerSystem";
    private static final String USER_RESOURCE = "User";
    private static final String ALL = "ALL";
    private static final String DEPARTMENT = "DEPARTMENT";
    private static final String OWN = "OWN";
    private static final String ALL_FIELDS = "{}";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionService rolePermissionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${security.bootstrap.superadmin-username:superadmin}")
    private String superadminUsername;

    @Value("${security.bootstrap.superadmin-password:superadmin}")
    private String superadminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialize() {
        log.info("Initializing default roles, permissions and bootstrap superadmin");

        Role superAdmin = role(SUPERADMIN, "Full system access (ALL scope, all fields)");
        Role admin = role(ADMIN, "Department-scoped administration");
        Role user = role(USER, "Self-service access to own resources");

        // Superadmin: full CRUD on everything, all fields.
        for (String resource : new String[]{COMPUTER_SYSTEM, USER_RESOURCE}) {
            assign(superAdmin, resource, GrantService.READ, ALL, ALL_FIELDS);
            assign(superAdmin, resource, GrantService.WRITE, ALL, ALL_FIELDS);
            assign(superAdmin, resource, GrantService.DELETE, ALL, ALL_FIELDS);
        }

        // Admin: department-scoped; may read all fields but not change identity/network fields.
        assign(admin, COMPUTER_SYSTEM, GrantService.READ, DEPARTMENT, ALL_FIELDS);
        assign(admin, COMPUTER_SYSTEM, GrantService.WRITE, DEPARTMENT,
                writable("hostname", "manufacturer", "model", "macAddress", "ipAddress"));
        assign(admin, COMPUTER_SYSTEM, GrantService.DELETE, DEPARTMENT, ALL_FIELDS);
        assign(admin, USER_RESOURCE, GrantService.READ, DEPARTMENT, ALL_FIELDS);
        assign(admin, USER_RESOURCE, GrantService.WRITE, DEPARTMENT, writable("email"));

        // Self-service user: read own records; write only own biographical fields.
        assign(user, USER_RESOURCE, GrantService.READ, OWN, ALL_FIELDS);
        assign(user, USER_RESOURCE, GrantService.WRITE, OWN, writable("email"));
        assign(user, COMPUTER_SYSTEM, GrantService.READ, OWN, ALL_FIELDS);

        rolePermissionService.reloadCache();
        bootstrapSuperadmin(superAdmin);

        log.info("Default roles, permissions and bootstrap superadmin initialized");
    }

    private void bootstrapSuperadmin(Role superAdmin) {
        if (userRepository.findByUsername(superadminUsername).isPresent()) {
            return;
        }
        User admin = User.builder()
                .username(superadminUsername)
                .email(superadminUsername + "@local")
                .passwordHash(passwordEncoder.encode(superadminPassword))
                .build();
        admin.getRoles().add(superAdmin);
        userRepository.save(admin);
        log.info("Created bootstrap superadmin user '{}'", superadminUsername);
    }

    private Role role(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() ->
                roleRepository.save(Role.builder().name(name).description(description).build()));
    }

    private void assign(Role role, String resourceType, String operation, String scope, String fieldPermissions) {
        var existing = permissionRepository.findByResourceTypeAndOperationAndScope(resourceType, operation, scope);
        Permission permission = existing.isEmpty()
                ? permissionRepository.save(Permission.builder()
                        .resourceType(resourceType).operation(operation).scope(scope)
                        .fieldPermissions(fieldPermissions).build())
                : existing.get(0);

        if (!rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
            rolePermissionRepository.save(RolePermission.builder().role(role).permission(permission).build());
        }
    }

    private String writable(String... fields) {
        Map<String, String> map = new java.util.HashMap<>();
        for (String field : fields) {
            map.put(field, "WRITE");
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize field permissions: {}", e.getMessage());
            return ALL_FIELDS;
        }
    }
}
