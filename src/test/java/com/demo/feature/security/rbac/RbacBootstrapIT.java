package com.demo.feature.security.rbac;

import com.demo.feature.user.User;
import com.demo.feature.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The startup seed has already run by the time this test executes (the
 * context is up), so the first test observes its result and the second proves
 * re-running it changes nothing. Deliberately not {@code @Transactional}: the
 * seed is committed state, not test data.
 */
@SpringBootTest
class RbacBootstrapIT {

    @Autowired
    private RbacBootstrap bootstrap;

    @Autowired
    private RbacProperties properties;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void seedsLockedSuperAdminRoleWithEveryOperationOnEverything() {
        Role superAdmin = roleRepository.findByName(properties.getBootstrap().getSuperAdminRole()).orElseThrow();

        assertTrue(superAdmin.isSystem());
        Set<String> keys = superAdmin.getPermissions().stream().map(Permission::key).collect(Collectors.toSet());
        assertEquals(Set.of("*:*:CREATE", "*:*:READ", "*:*:UPDATE", "*:*:DELETE"), keys);
    }

    @Test
    void seedsBootstrapUserWithOneGlobalGrantAndIsIdempotent() {
        User admin = userRepository.findByUsername(properties.getBootstrap().getUsername()).orElseThrow();
        Role superAdmin = roleRepository.findByName(properties.getBootstrap().getSuperAdminRole()).orElseThrow();

        bootstrap.seed();
        bootstrap.seed();

        List<RoleAssignment> grants = roleAssignmentRepository.findByUserId(admin.getId());
        assertEquals(1, grants.stream().filter(g -> g.getRole().getId().equals(superAdmin.getId())).count());
        assertTrue(grants.stream().allMatch(RoleAssignment::isGlobal));
        assertEquals(4, roleRepository.findByName(superAdmin.getName()).orElseThrow().getPermissions().size());
    }
}
