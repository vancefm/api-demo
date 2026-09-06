package com.demo.feature.security.rbac;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentService;
import com.demo.feature.user.User;
import com.demo.feature.user.UserManagementService;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock
    private RoleAssignmentRepository repository;

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private RoleService roleService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private AccessControl accessControl;

    private RoleAssignmentService service;

    private User alice;
    private Role role;
    private Department it;

    @BeforeEach
    void setUp() {
        service = new RoleAssignmentService(repository, new RoleAssignmentMapper(),
            userManagementService, roleService, departmentService, accessControl);
        // Permissive access control; denial cases stub the specific call.
        lenient().when(accessControl.filterReadable(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));
        lenient().when(accessControl.isAllowed(anyString(), any(), any())).thenReturn(true);
        alice = User.builder().id(7L).username("alice").email("alice@example.com").build();
        role = Role.builder().id(2L).name("Department User").build();
        it = Department.builder().id(1L).name("IT").build();
    }

    @Test
    void grant_departmentScoped() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        when(roleService.resolveRole(2L)).thenReturn(role);
        when(departmentService.resolveDepartment(1L)).thenReturn(it);
        when(repository.save(any(RoleAssignment.class))).thenAnswer(inv -> {
            RoleAssignment saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        RoleAssignmentDto result = service.grant(7L, RoleAssignmentDto.builder().roleId(2L).departmentId(1L).build());

        assertEquals(99L, result.getId());
        assertEquals(7L, result.getUserId());
        assertEquals("Department User", result.getRoleName());
        assertEquals("IT", result.getDepartmentName());
    }

    @Test
    void grant_globalWhenDepartmentOmitted() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        when(roleService.resolveRole(2L)).thenReturn(role);
        when(repository.save(any(RoleAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleAssignmentDto result = service.grant(7L, RoleAssignmentDto.builder().roleId(2L).build());

        ArgumentCaptor<RoleAssignment> saved = ArgumentCaptor.forClass(RoleAssignment.class);
        verify(repository).save(saved.capture());
        assertNull(saved.getValue().getDepartment());
        assertNull(result.getDepartmentId());
        verify(departmentService, never()).resolveDepartment(any());
    }

    @Test
    void grant_duplicateGlobalIs409() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        when(roleService.resolveRole(2L)).thenReturn(role);
        when(repository.existsByUserIdAndRoleIdAndDepartmentIsNull(7L, 2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> service.grant(7L, RoleAssignmentDto.builder().roleId(2L).build()));
        verify(repository, never()).save(any());
    }

    @Test
    void grant_duplicateDepartmentScopedIs409() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        when(roleService.resolveRole(2L)).thenReturn(role);
        when(departmentService.resolveDepartment(1L)).thenReturn(it);
        when(repository.existsByUserIdAndRoleIdAndDepartmentId(7L, 2L, 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> service.grant(7L, RoleAssignmentDto.builder().roleId(2L).departmentId(1L).build()));
    }

    @Test
    void grant_unknownRolePropagates404() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        when(roleService.resolveRole(99L)).thenThrow(new ResourceNotFoundException("Role with id 99 not found"));

        assertThrows(ResourceNotFoundException.class,
            () -> service.grant(7L, RoleAssignmentDto.builder().roleId(99L).build()));
    }

    @Test
    void list_sortedById() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        RoleAssignment second = RoleAssignment.builder().id(2L).user(alice).role(role).department(it).build();
        RoleAssignment first = RoleAssignment.builder().id(1L).user(alice).role(role).build();
        when(repository.findByUserId(7L)).thenReturn(List.of(second, first));

        List<RoleAssignmentDto> result = service.list(7L);

        assertEquals(List.of(1L, 2L), result.stream().map(RoleAssignmentDto::getId).toList());
    }

    @Test
    void revoke_deletesOwnAssignment() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        RoleAssignment assignment = RoleAssignment.builder().id(5L).user(alice).role(role).build();
        when(repository.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(assignment));

        service.revoke(7L, 5L);

        verify(repository).delete(assignment);
    }

    @Test
    void grant_deniedBeforeAnyLookup() {
        doThrow(new AccessDeniedException("Not permitted to CREATE RoleAssignment (requires a global grant)"))
            .when(accessControl).requireAccess(eq("RoleAssignment"), eq(Operation.CREATE), any());

        assertThrows(AccessDeniedException.class,
            () -> service.grant(7L, RoleAssignmentDto.builder().roleId(2L).build()));

        verify(userManagementService, never()).resolveUser(any());
        verify(repository, never()).save(any());
    }

    @Test
    void list_hidesGrantsTheCallerMayNotRead() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        RoleAssignment global = RoleAssignment.builder().id(1L).user(alice).role(role).build();
        RoleAssignment scoped = RoleAssignment.builder().id(2L).user(alice).role(role).department(it).build();
        when(repository.findByUserId(7L)).thenReturn(List.of(global, scoped));
        when(accessControl.scopeOf(eq("RoleAssignment"), any())).thenAnswer(inv -> {
            RoleAssignmentDto dto = inv.getArgument(1);
            return dto.getDepartmentId() == null ? java.util.Set.of() : java.util.Set.of(dto.getDepartmentId());
        });
        when(accessControl.isAllowed(eq("RoleAssignment"), eq(Operation.READ), any()))
            .thenAnswer(inv -> !((java.util.Set<?>) inv.getArgument(2)).isEmpty()); // only department-scoped

        List<RoleAssignmentDto> result = service.list(7L);

        assertEquals(List.of(2L), result.stream().map(RoleAssignmentDto::getId).toList());
    }

    @Test
    void revoke_assignmentOfAnotherUserIs404() {
        when(userManagementService.resolveUser(7L)).thenReturn(alice);
        when(repository.findByIdAndUserId(5L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.revoke(7L, 5L));
        verify(repository, never()).delete(any());
    }
}
