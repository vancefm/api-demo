package com.demo.feature.user;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentMapper;
import com.demo.feature.department.DepartmentService;
import com.demo.feature.security.rbac.AccessControl;
import com.demo.feature.security.rbac.Operation;
import com.demo.feature.security.rbac.RoleAssignmentMapper;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private AccessControl accessControl;

    private UserManagementService service;

    private User existing;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(userRepository,
            new UserMapper(new DepartmentMapper(), new RoleAssignmentMapper()), departmentService, accessControl);
        // Permissive by default: the require* methods are no-ops on the mock,
        // readableDepartments() yields Optional.empty() (global) and masking is
        // an identity. Denial cases stub the specific call.
        lenient().when(accessControl.filterReadable(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));

        existing = User.builder()
            .id(1L)
            .username("john.doe")
            .email("john.doe@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();
    }

    private static UserDto dto(String username, String email, String firstName, String lastName) {
        UserDto dto = new UserDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        return dto;
    }

    @Test
    void createUser_persistsProfileFieldsAndDepartments() {
        Department it = Department.builder().id(5L).name("IT").build();
        UserDto request = dto("jane.doe", "jane@example.com", "Jane", "Doe");
        request.setDepartmentIds(List.of(5L));
        when(departmentService.resolveDepartments(List.of(5L))).thenReturn(Set.of(it));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = service.createUser(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("Jane", saved.getValue().getFirstName());
        assertEquals("Doe", saved.getValue().getLastName());
        assertEquals(1, saved.getValue().getDepartmentLinks().size());
        assertEquals("Jane", result.getFirstName());
        assertEquals(List.of(5L), result.getDepartmentIds());
    }

    @Test
    void createUser_duplicateUsernameRejected() {
        when(userRepository.existsByUsername("john.doe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> service.createUser(dto("john.doe", "other@example.com", null, null)));

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_unknownManagerIs404() {
        UserDto request = dto("jane.doe", "jane@example.com", null, null);
        request.setManagerId(99L);
        when(departmentService.resolveDepartments(null)).thenReturn(Set.of());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createUser(request));
    }

    @Test
    void updateUser_changesProfileFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentService.resolveDepartments(anyList())).thenReturn(Set.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserDto request = dto("john.doe", "john.doe@example.com", "Johnny", "Doe-Smith");
        request.setDepartmentIds(List.of());

        UserDto result = service.updateUser(1L, request);

        assertEquals("Johnny", result.getFirstName());
        assertEquals("Doe-Smith", result.getLastName());
        assertNull(result.getManagerId());
    }

    @Test
    void updateUser_emailChangedToExistingOneRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> service.updateUser(1L, dto("john.doe", "taken@example.com", null, null)));
    }

    @Test
    void findOrProvision_returnsExistingRowUntouched() {
        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.of(existing));

        User result = service.findOrProvision("john.doe", "ignored@example.com", "Ignored", "Ignored");

        assertSame(existing, result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrProvision_createsRowFromDirectoryAttributes() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.findOrProvision("user1", "user1@example.com", "User", "One");

        assertEquals("user1", result.getUsername());
        assertEquals("user1@example.com", result.getEmail());
        assertEquals("User", result.getFirstName());
        assertEquals("One", result.getLastName());
        assertNull(result.getManager());
    }

    @Test
    void deleteUser_notFoundIs404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUser(99L));
    }

    @Test
    void deleteUser_deniedByAccessControlDeletesNothing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("Not permitted to DELETE User"))
            .when(accessControl).requireAccess(eq("User"), eq(Operation.DELETE), any());

        assertThrows(AccessDeniedException.class, () -> service.deleteUser(1L));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void createUser_deniedBeforeAnyLookup() {
        doThrow(new AccessDeniedException("Not permitted to CREATE User"))
            .when(accessControl).requireAccess(eq("User"), eq(Operation.CREATE), any());

        assertThrows(AccessDeniedException.class,
            () -> service.createUser(dto("jane.doe", "jane@example.com", null, null)));

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_fieldDenialHappensAfterUnreadableFieldsAreRetainedAndBeforeSaving() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("Not permitted to UPDATE User field(s) [email]"))
            .when(accessControl).requireFieldAccess(eq("User"), eq(Operation.UPDATE), any(), any());

        assertThrows(AccessDeniedException.class,
            () -> service.updateUser(1L, dto("john.doe", "new@example.com", "John", "Doe")));

        InOrder order = inOrder(accessControl);
        order.verify(accessControl).requireAccess(eq("User"), eq(Operation.UPDATE), any());
        order.verify(accessControl).retainUnreadable(eq("User"), any(), any(), any());
        order.verify(accessControl).requireFieldAccess(eq("User"), eq(Operation.UPDATE), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAllUsers_scopesToReadableDepartments() {
        when(accessControl.readableDepartments("User")).thenReturn(Optional.of(Set.of(5L)));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(existing)));

        service.getAllUsers(PageRequest.of(0, 10));

        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(accessControl).filterReadable(eq("User"), any(UserDto.class));
    }
}
