package com.demo.feature.security.rbac;

import com.demo.feature.user.UserDto;
import com.demo.platform.exception.ConflictException;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.InvalidRequestException;
import com.demo.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository repository;

    @Mock
    private AccessControl accessControl;

    private RoleService service;

    private Role editable;
    private Role system;

    @BeforeEach
    void setUp() {
        SecuredEntityRegistry registry = new SecuredEntityRegistry(List.of(
            SecuredEntity.departmental("User", UserDto.class, UserDto::getDepartmentIds),
            SecuredEntity.global("Role", RoleDto.class)));
        service = new RoleService(repository, new RoleMapper(), registry, accessControl);
        // Permissive access control; denial cases stub the specific call.
        lenient().when(accessControl.filterReadable(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));

        editable = Role.builder().id(1L).name("Department User").build();
        system = Role.builder().id(2L).name("SuperAdmin").system(true).build();
        system.addPermission("*", "*", Operation.READ);

        lenient().when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveAndFlush(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PermissionDto perm(String entity, String field, Operation op) {
        return PermissionDto.builder().entity(entity).field(field).operation(op).build();
    }

    private static Set<String> keys(Role role) {
        return role.getPermissions().stream().map(Permission::key).collect(Collectors.toSet());
    }

    @Test
    void createRole_withInlinePermissions() {
        RoleDto request = RoleDto.builder().name("Reader")
            .permissions(List.of(perm("User", "firstName", Operation.READ), perm("User", null, Operation.DELETE)))
            .build();

        RoleDto result = service.createRole(request);

        assertEquals("Reader", result.getName());
        assertEquals(2, result.getPermissions().size());
        assertTrue(result.getPermissions().stream().anyMatch(p -> "*".equals(p.getField())
            && p.getOperation() == Operation.DELETE), "blank field defaults to *");
    }

    @Test
    void createRole_duplicateName() {
        when(repository.existsByName("Reader")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> service.createRole(RoleDto.builder().name("Reader").build()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRole_rejectsUnknownEntityFieldAndBadShapes() {
        assertThrows(InvalidRequestException.class, () -> service.createRole(RoleDto.builder().name("x")
            .permissions(List.of(perm("Widget", "*", Operation.READ))).build()));
        assertThrows(InvalidRequestException.class, () -> service.createRole(RoleDto.builder().name("x")
            .permissions(List.of(perm("User", "password", Operation.READ))).build()));
        assertThrows(InvalidRequestException.class, () -> service.createRole(RoleDto.builder().name("x")
            .permissions(List.of(perm("User", "email", Operation.DELETE))).build()));
        assertThrows(InvalidRequestException.class, () -> service.createRole(RoleDto.builder().name("x")
            .permissions(List.of(perm("*", "email", Operation.READ))).build()));
        assertThrows(InvalidRequestException.class, () -> service.createRole(RoleDto.builder().name("x")
            .permissions(List.of(perm("User", "email", null))).build()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateRole_systemRoleCannotBeRenamedButDescriptionMayChange() {
        when(repository.findById(2L)).thenReturn(Optional.of(system));

        assertThrows(ConflictException.class,
            () -> service.updateRole(2L, RoleDto.builder().name("Root").build()));

        RoleDto result = service.updateRole(2L, RoleDto.builder().name("SuperAdmin").description("all").build());
        assertEquals("all", result.getDescription());
    }

    @Test
    void updateRole_renameToExistingNameRejected() {
        when(repository.findById(1L)).thenReturn(Optional.of(editable));
        when(repository.existsByName("Taken")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> service.updateRole(1L, RoleDto.builder().name("Taken").build()));
    }

    @Test
    void deleteRole_systemRoleRefused() {
        when(repository.findById(2L)).thenReturn(Optional.of(system));

        assertThrows(ConflictException.class, () -> service.deleteRole(2L));
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRole_ordinaryRoleDeleted() {
        when(repository.findById(1L)).thenReturn(Optional.of(editable));

        service.deleteRole(1L);

        verify(repository).delete(editable);
    }

    @Test
    void deleteRole_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteRole(99L));
    }

    @Test
    void replacePermissions_diffsKeepingSurvivorsAndCollapsingDuplicates() {
        Permission surviving = editable.addPermission("User", "firstName", Operation.READ);
        editable.addPermission("User", "lastName", Operation.READ);
        when(repository.findById(1L)).thenReturn(Optional.of(editable));

        service.replacePermissions(1L, List.of(
            perm("User", "firstName", Operation.READ),
            perm("User", "firstName", Operation.READ),
            perm("User", "email", Operation.UPDATE)));

        assertEquals(Set.of("User:firstName:READ", "User:email:UPDATE"), keys(editable));
        assertTrue(editable.getPermissions().contains(surviving), "surviving grant keeps its identity");
        assertSame(surviving, editable.getPermissions().stream()
            .filter(p -> "firstName".equals(p.getField())).findFirst().orElseThrow());
    }

    @Test
    void replacePermissions_systemRoleRefused() {
        when(repository.findById(2L)).thenReturn(Optional.of(system));

        assertThrows(ConflictException.class,
            () -> service.replacePermissions(2L, List.of(perm("User", "*", Operation.READ))));
        assertEquals(Set.of("*:*:READ"), keys(system));
    }

    @Test
    void addPermission_addsOnceThenConflicts() {
        when(repository.findById(1L)).thenReturn(Optional.of(editable));

        PermissionDto added = service.addPermission(1L, perm("User", "email", Operation.READ));
        assertEquals("email", added.getField());
        assertEquals(Set.of("User:email:READ"), keys(editable));

        assertThrows(DuplicateResourceException.class,
            () -> service.addPermission(1L, perm("User", "email", Operation.READ)));
    }

    @Test
    void replacePermissions_deniedWithoutUpdateOnPermissionsField() {
        editable.addPermission("User", "firstName", Operation.READ);
        when(repository.findById(1L)).thenReturn(Optional.of(editable));
        doThrow(new AccessDeniedException("Not permitted to UPDATE Role field(s) [permissions]"))
            .when(accessControl).requireFieldAccess(eq("Role"), eq(Operation.UPDATE), any(), any());

        assertThrows(AccessDeniedException.class,
            () -> service.replacePermissions(1L, List.of(perm("User", "email", Operation.READ))));

        assertEquals(Set.of("User:firstName:READ"), keys(editable), "nothing changed");
        verify(repository, never()).flush();
    }

    @Test
    void createRole_deniedNeedsGlobalGrant() {
        doThrow(new AccessDeniedException("Not permitted to CREATE Role (requires a global grant)"))
            .when(accessControl).requireAccess(eq("Role"), eq(Operation.CREATE), eq(Set.of()));

        assertThrows(AccessDeniedException.class,
            () -> service.createRole(RoleDto.builder().name("x").build()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void removePermission_unknownIdIs404() {
        when(repository.findById(1L)).thenReturn(Optional.of(editable));

        assertThrows(ResourceNotFoundException.class, () -> service.removePermission(1L, 123L));
    }

    @Test
    void removePermission_removesMatchingGrant() {
        Permission keep = editable.addPermission("User", "firstName", Operation.READ);
        keep.setId(10L);
        Permission drop = editable.addPermission("User", "lastName", Operation.READ);
        drop.setId(11L);
        when(repository.findById(1L)).thenReturn(Optional.of(editable));

        service.removePermission(1L, 11L);

        assertEquals(Set.of("User:firstName:READ"), keys(editable));
    }
}
