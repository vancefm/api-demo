package com.demo.feature.security.rbac;

import com.demo.feature.department.Department;
import com.demo.feature.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The decision rules, in isolation. IT = department 1, HR = department 2.
 */
class EffectivePermissionsTest {

    private static final Department IT = Department.builder().id(1L).name("IT").build();
    private static final Department HR = Department.builder().id(2L).name("HR").build();
    private static final User ALICE = User.builder().id(7L).username("alice").email("a@example.com").build();

    private static Role role(String name, String[]... grants) {
        Role role = Role.builder().id((long) name.hashCode()).name(name).build();
        for (String[] grant : grants) {
            role.addPermission(grant[0], grant[1], Operation.valueOf(grant[2]));
        }
        return role;
    }

    private static RoleAssignment assign(Role role, Department department) {
        return RoleAssignment.builder().user(ALICE).role(role).department(department).build();
    }

    private static final Role DEPARTMENT_USER = role("Department User",
        new String[] {"User", "firstName", "READ"},
        new String[] {"User", "lastName", "READ"},
        new String[] {"User", "lastName", "UPDATE"});

    private static final Role SUPER_ADMIN = role("SuperAdmin",
        new String[] {"*", "*", "CREATE"},
        new String[] {"*", "*", "READ"},
        new String[] {"*", "*", "UPDATE"},
        new String[] {"*", "*", "DELETE"});

    @Test
    void noAssignmentsAllowNothing() {
        EffectivePermissions none = EffectivePermissions.of(List.of());

        assertTrue(none.isEmpty());
        assertFalse(none.allows("User", Operation.READ, Set.of(1L)));
        assertFalse(none.allows("User", Operation.READ, Set.of()));
        assertEquals(Optional.of(Set.of()), none.readableDepartments("User"));
    }

    @ParameterizedTest(name = "[{index}] {0} on User in {1} → {2}")
    @CsvSource({
        // op,     targets, expected
        "READ,     1,       true",    // scoped grant, matching department
        "READ,     2,       false",   // scoped grant, other department
        "READ,     '1;2',   true",    // target in several departments, one matches
        "READ,     '',      false",   // non-departmental target needs a global grant
        "UPDATE,   1,       true",    // field-level UPDATE grant gives entity-level UPDATE access
        "DELETE,   1,       false",   // never granted
        "CREATE,   1,       false",
    })
    void departmentScopedGrant(String op, String targets, boolean expected) {
        EffectivePermissions permissions = EffectivePermissions.of(List.of(assign(DEPARTMENT_USER, IT)));

        assertEquals(expected, permissions.allows("User", Operation.valueOf(op), parse(targets)));
    }

    @Test
    void departmentScopedGrantDoesNotReachOtherEntities() {
        EffectivePermissions permissions = EffectivePermissions.of(List.of(assign(DEPARTMENT_USER, IT)));

        assertFalse(permissions.allows("ComputerSystem", Operation.READ, Set.of(1L)));
    }

    @Test
    void fieldAccessFollowsTheGrantedFields() {
        EffectivePermissions permissions = EffectivePermissions.of(List.of(assign(DEPARTMENT_USER, IT)));

        assertTrue(permissions.allowsField("User", "firstName", Operation.READ, Set.of(1L)));
        assertTrue(permissions.allowsField("User", "lastName", Operation.READ, Set.of(1L)));
        assertFalse(permissions.allowsField("User", "email", Operation.READ, Set.of(1L)));
        assertTrue(permissions.allowsField("User", "lastName", Operation.UPDATE, Set.of(1L)));
        assertFalse(permissions.allowsField("User", "firstName", Operation.UPDATE, Set.of(1L)));
        assertFalse(permissions.allowsField("User", "firstName", Operation.READ, Set.of(2L)), "wrong department");
    }

    @Test
    void globalWildcardGrantAllowsEverythingEverywhere() {
        EffectivePermissions permissions = EffectivePermissions.of(List.of(assign(SUPER_ADMIN, null)));

        for (Operation op : Operation.values()) {
            assertTrue(permissions.allows("User", op, Set.of()));
            assertTrue(permissions.allows("Role", op, Set.of()));
            assertTrue(permissions.allows("ComputerSystem", op, Set.of(1L, 2L)));
            assertTrue(permissions.allowsField("User", "email", op, Set.of(2L)));
        }
        assertEquals(Optional.empty(), permissions.readableDepartments("Role"));
    }

    @Test
    void departmentScopedWildcardIsStillBoundToItsDepartment() {
        EffectivePermissions permissions = EffectivePermissions.of(List.of(assign(SUPER_ADMIN, HR)));

        assertTrue(permissions.allows("User", Operation.DELETE, Set.of(2L)));
        assertFalse(permissions.allows("User", Operation.DELETE, Set.of(1L)));
        assertFalse(permissions.allows("Role", Operation.READ, Set.of()), "roles are global-only");
        assertEquals(Optional.of(Set.of(2L)), permissions.readableDepartments("User"));
    }

    @Test
    void grantsFromSeveralAssignmentsAccumulate() {
        EffectivePermissions permissions = EffectivePermissions.of(List.of(
            assign(DEPARTMENT_USER, IT),
            assign(DEPARTMENT_USER, HR)));

        assertEquals(Optional.of(Set.of(1L, 2L)), permissions.readableDepartments("User"));
        assertTrue(permissions.allowsField("User", "lastName", Operation.UPDATE, Set.of(2L)));
        assertEquals(Optional.of(Set.of()), permissions.readableDepartments("ComputerSystem"));
    }

    @Test
    void readableDepartmentsCountsFieldLevelGrants() {
        Role peek = role("Peek", new String[] {"ComputerSystem", "hostname", "READ"});
        EffectivePermissions permissions = EffectivePermissions.of(List.of(assign(peek, IT)));

        assertEquals(Optional.of(Set.of(1L)), permissions.readableDepartments("ComputerSystem"));
    }

    private static Set<Long> parse(String targets) {
        if (targets == null || targets.isBlank()) {
            return Set.of();
        }
        return Set.of(targets.split(";")).stream().map(Long::valueOf).collect(java.util.stream.Collectors.toSet());
    }
}
