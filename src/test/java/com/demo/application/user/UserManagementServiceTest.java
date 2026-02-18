package com.demo.application.user;

import com.demo.application.security.auth.RoleManagementService;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.User;
import com.demo.domain.user.UserDto;
import com.demo.domain.user.UserMapper;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleManagementService roleManagementService;

    private UserMapper userMapper;

    private UserManagementService service;

    private User testUser;
    private UserDto testDto;
    private Role testRole;
    private User managerUser;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> implClass = Class.forName(UserMapper.class.getName() + "Impl");
        userMapper = (UserMapper) implClass.getDeclaredConstructor().newInstance();
        service = new UserManagementService(userRepository, roleManagementService, userMapper);

        testRole = Role.builder()
                .id(1L)
                .name("MY_APP_USER")
                .description("Standard user role")
                .build();

        managerUser = User.builder()
                .id(2L)
                .username("manager.user")
                .email("manager@example.com")
                .department("IT")
                .role(testRole)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("john.doe")
                .email("john.doe@example.com")
                .department("IT")
                .role(testRole)
                .build();

        testDto = new UserDto();
        testDto.setId(1L);
        testDto.setUsername("john.doe");
        testDto.setEmail("john.doe@example.com");
        testDto.setDepartment("IT");
        testDto.setRoleId(1L);
    }

    // ===== createUser =====

    @Test
    void testCreateUser_Success() {
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(roleManagementService.resolveRole(1L)).thenReturn(testRole);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = service.createUser(testDto);

        assertNotNull(result);
        assertEquals("john.doe", result.getUsername());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("IT", result.getDepartment());
        assertEquals(1L, result.getRoleId());
        assertEquals("MY_APP_USER", result.getRoleName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateUser_WithManager() {
        testDto.setManagerId(2L);

        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(roleManagementService.resolveRole(1L)).thenReturn(testRole);
        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));

        User savedUser = User.builder()
                .id(1L)
                .username("john.doe")
                .email("john.doe@example.com")
                .department("IT")
                .role(testRole)
                .manager(managerUser)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = service.createUser(testDto);

        assertNotNull(result);
        assertEquals(2L, result.getManagerId());
        verify(userRepository, times(1)).findById(2L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateUser_ManagerNotFound() {
        testDto.setManagerId(99L);

        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(roleManagementService.resolveRole(1L)).thenReturn(testRole);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createUser(testDto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateUsername() {
        when(userRepository.existsByUsername("john.doe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.createUser(testDto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.createUser(testDto));

        verify(userRepository, never()).save(any(User.class));
    }

    // ===== getAllUsers =====

    @Test
    void testGetAllUsers() {
        User secondUser = User.builder()
                .id(2L)
                .username("jane.doe")
                .email("jane.doe@example.com")
                .department("HR")
                .role(testRole)
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, secondUser));

        List<UserDto> result = service.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("john.doe", result.get(0).getUsername());
        assertEquals("jane.doe", result.get(1).getUsername());
    }

    @Test
    void testGetAllUsers_EmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDto> result = service.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===== getUserById =====

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = service.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john.doe", result.getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUserById(99L));
    }

    // ===== updateUser =====

    @Test
    void testUpdateUser_Success() {
        UserDto updateDto = new UserDto();
        updateDto.setUsername("john.doe");
        updateDto.setEmail("john.updated@example.com");
        updateDto.setDepartment("Engineering");
        updateDto.setRoleId(1L);

        User updatedUser = User.builder()
                .id(1L)
                .username("john.doe")
                .email("john.updated@example.com")
                .department("Engineering")
                .role(testRole)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("john.updated@example.com")).thenReturn(false);
        when(roleManagementService.resolveRole(1L)).thenReturn(testRole);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserDto result = service.updateUser(1L, updateDto);

        assertNotNull(result);
        assertEquals("john.updated@example.com", result.getEmail());
        assertEquals("Engineering", result.getDepartment());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateUser_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateUser(99L, testDto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_DuplicateUsername() {
        UserDto updateDto = new UserDto();
        updateDto.setUsername("existing.user");
        updateDto.setEmail("john.doe@example.com");
        updateDto.setDepartment("IT");
        updateDto.setRoleId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existing.user")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.updateUser(1L, updateDto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_DuplicateEmail() {
        UserDto updateDto = new UserDto();
        updateDto.setUsername("john.doe");
        updateDto.setEmail("existing@example.com");
        updateDto.setDepartment("IT");
        updateDto.setRoleId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.updateUser(1L, updateDto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_WithManager() {
        UserDto updateDto = new UserDto();
        updateDto.setUsername("john.doe");
        updateDto.setEmail("john.doe@example.com");
        updateDto.setDepartment("IT");
        updateDto.setRoleId(1L);
        updateDto.setManagerId(2L);

        User updatedUser = User.builder()
                .id(1L)
                .username("john.doe")
                .email("john.doe@example.com")
                .department("IT")
                .role(testRole)
                .manager(managerUser)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleManagementService.resolveRole(1L)).thenReturn(testRole);
        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserDto result = service.updateUser(1L, updateDto);

        assertNotNull(result);
        assertEquals(2L, result.getManagerId());
    }

    @Test
    void testUpdateUser_ClearManager() {
        testUser.setManager(managerUser);

        UserDto updateDto = new UserDto();
        updateDto.setUsername("john.doe");
        updateDto.setEmail("john.doe@example.com");
        updateDto.setDepartment("IT");
        updateDto.setRoleId(1L);
        updateDto.setManagerId(null);

        User updatedUser = User.builder()
                .id(1L)
                .username("john.doe")
                .email("john.doe@example.com")
                .department("IT")
                .role(testRole)
                .manager(null)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleManagementService.resolveRole(1L)).thenReturn(testRole);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserDto result = service.updateUser(1L, updateDto);

        assertNotNull(result);
        assertNull(result.getManagerId());
    }

    // ===== deleteUser =====

    @Test
    void testDeleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        service.deleteUser(1L);

        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void testDeleteUser_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUser(99L));

        verify(userRepository, never()).delete(any(User.class));
    }
}
