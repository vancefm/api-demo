package com.demo.feature.security.rbac;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentDto;
import com.demo.feature.security.auth.CurrentUser;
import com.demo.feature.security.auth.UserPrincipal;
import com.demo.feature.user.User;
import com.demo.feature.user.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlTest {

    private static final UserPrincipal ALICE = new UserPrincipal(7L, "alice");
    private static final Department IT = Department.builder().id(1L).name("IT").build();

    @Mock
    private CurrentUser currentUser;

    @Mock
    private RoleAssignmentRepository repository;

    private AccessControl accessControl;

    @BeforeEach
    void setUp() {
        SecuredEntityRegistry registry = new SecuredEntityRegistry(List.of(
            SecuredEntity.departmental("User", UserDto.class, UserDto::getDepartmentIds),
            new SecuredEntity<>("Department", DepartmentDto.class,
                dto -> dto.getId() == null ? Set.of() : Set.of(dto.getId())),
            SecuredEntity.global("Role", RoleDto.class)));
        accessControl = new AccessControl(currentUser, repository, registry);
    }

    private void aliceHolds(Department department, String[]... grants) {
        Role role = Role.builder().id(2L).name("Role").build();
        for (String[] grant : grants) {
            role.addPermission(grant[0], grant[1], Operation.valueOf(grant[2]));
        }
        User alice = User.builder().id(7L).username("alice").email("a@example.com").build();
        when(currentUser.require()).thenReturn(ALICE);
        when(repository.findByUserId(7L)).thenReturn(List.of(
            RoleAssignment.builder().user(alice).role(role).department(department).build()));
    }

    @Test
    void requireAccess_passesWithinScopeAndDeniesOutside() {
        aliceHolds(IT, new String[] {"User", "firstName", "READ"});

        assertDoesNotThrow(() -> accessControl.requireAccess("User", Operation.READ, Set.of(1L)));

        AccessDeniedException otherDepartment = assertThrows(AccessDeniedException.class,
            () -> accessControl.requireAccess("User", Operation.READ, Set.of(2L)));
        assertEquals("Not permitted to READ User in department(s) [2]", otherDepartment.getMessage());

        AccessDeniedException nonDepartmental = assertThrows(AccessDeniedException.class,
            () -> accessControl.requireAccess("User", Operation.READ, Set.of()));
        assertEquals("Not permitted to READ User (requires a global grant)", nonDepartmental.getMessage());

        assertThrows(AccessDeniedException.class,
            () -> accessControl.requireAccess("User", Operation.DELETE, Set.of(1L)));
    }

    @Test
    void requireFieldAccess_namesTheOffendingFields() {
        aliceHolds(IT, new String[] {"User", "firstName", "UPDATE"});

        assertDoesNotThrow(() -> accessControl.requireFieldAccess("User", Operation.UPDATE, List.of("firstName"), Set.of(1L)));

        AccessDeniedException denied = assertThrows(AccessDeniedException.class, () ->
            accessControl.requireFieldAccess("User", Operation.UPDATE, List.of("firstName", "email", "username"), Set.of(1L)));
        assertEquals("Not permitted to UPDATE User field(s) [email, username] in department(s) [1]", denied.getMessage());
    }

    @Test
    void readableDepartments_scopedVersusGlobal() {
        aliceHolds(IT, new String[] {"User", "*", "READ"});
        assertEquals(Optional.of(Set.of(1L)), accessControl.readableDepartments("User"));
        assertEquals(Optional.of(Set.of()), accessControl.readableDepartments("Role"));
    }

    @Test
    void readableDepartments_globalIsUnrestricted() {
        aliceHolds(null, new String[] {"*", "*", "READ"});
        assertEquals(Optional.empty(), accessControl.readableDepartments("User"));
    }

    @Test
    void filterReadable_masksUsingTheDtoOwnScope() {
        aliceHolds(IT, new String[] {"User", "firstName", "READ"}, new String[] {"User", "lastName", "READ"});
        UserDto inIt = new UserDto();
        inIt.setId(5L);
        inIt.setUsername("bob");
        inIt.setFirstName("Bob");
        inIt.setLastName("Builder");
        inIt.setDepartmentIds(List.of(1L));

        accessControl.filterReadable("User", inIt);

        assertEquals(5L, inIt.getId());
        assertEquals("Bob", inIt.getFirstName());
        assertNull(inIt.getUsername());
        assertNull(inIt.getDepartmentIds(), "departmentIds itself is not readable and gets masked too");
    }

    @Test
    void readableFields_wildcardGrantReadsEverything() {
        aliceHolds(null, new String[] {"*", "*", "READ"});

        Set<String> fields = accessControl.readableFields("User", Set.of());

        assertTrue(fields.containsAll(Set.of("id", "username", "email", "firstName", "lastName",
            "departmentIds", "departments", "managerId", "roleAssignments")));
    }

    @Test
    void retainUnreadable_copiesStoredValuesOfMaskedFields() {
        aliceHolds(IT, new String[] {"User", "firstName", "READ"}, new String[] {"User", "firstName", "UPDATE"});
        UserDto stored = new UserDto();
        stored.setUsername("bob");
        stored.setEmail("bob@example.com");
        stored.setFirstName("Bob");
        UserDto incoming = new UserDto();
        incoming.setFirstName("Robert");

        accessControl.retainUnreadable("User", stored, incoming, Set.of(1L));

        assertEquals("bob", incoming.getUsername());
        assertEquals("bob@example.com", incoming.getEmail());
        assertEquals("Robert", incoming.getFirstName());
    }

    @Test
    void anonymousCallerIsRejectedBeforeAnyLookup() {
        when(currentUser.require()).thenThrow(new AuthenticationCredentialsNotFoundException("none"));

        assertThrows(AuthenticationCredentialsNotFoundException.class,
            () -> accessControl.requireAccess("User", Operation.READ, Set.of()));
    }
}
