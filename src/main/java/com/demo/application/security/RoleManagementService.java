package com.demo.application.security;

import com.demo.domain.security.Permission;
import com.demo.domain.security.Role;
import com.demo.domain.security.RolePermission;
import com.demo.domain.user.User;
import com.demo.domain.security.dto.PermissionDto;
import com.demo.domain.security.dto.RoleDto;
import com.demo.domain.user.UserDto;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import com.demo.shared.security.RolePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing roles, permissions, and users.
 */
@Service
@Transactional
public class RoleManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleManagementService.class);
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final RolePermissionService rolePermissionService;
    
    public RoleManagementService(RoleRepository roleRepository,
                                PermissionRepository permissionRepository,
                                RolePermissionRepository rolePermissionRepository,
                                UserRepository userRepository,
                                RolePermissionService rolePermissionService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.rolePermissionService = rolePermissionService;
    }
    
    // ===== Role Management =====
    
    public RoleDto createRole(RoleDto dto) {
        if (roleRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Role with name '" + dto.getName() + "' already exists");
        }
        
        Role role = Role.builder()
            .name(dto.getName())
            .hierarchyLevel(dto.getHierarchyLevel())
            .description(dto.getDescription())
            .build();
        
        Role saved = roleRepository.save(role);
        logger.info("Created role: {}", saved.getName());
        
        return mapToRoleDto(saved);
    }
    
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
            .map(this::mapToRoleDto)
            .collect(Collectors.toList());
    }
    
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        return mapToRoleDto(role);
    }
    
    public RoleDto updateRole(Long id, RoleDto dto) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        
        // Check if name is being changed to an existing name
        if (!role.getName().equals(dto.getName()) && roleRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Role with name '" + dto.getName() + "' already exists");
        }
        
        role.setName(dto.getName());
        role.setHierarchyLevel(dto.getHierarchyLevel());
        role.setDescription(dto.getDescription());
        
        Role updated = roleRepository.save(role);
        logger.info("Updated role: {}", updated.getName());
        
        // Reload cache after role update
        rolePermissionService.reloadCache();
        
        return mapToRoleDto(updated);
    }
    
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        
        // Delete all role-permission mappings
        rolePermissionRepository.deleteByRole(role);
        
        roleRepository.delete(role);
        logger.info("Deleted role: {}", role.getName());
        
        // Reload cache after role deletion
        rolePermissionService.reloadCache();
    }
    
    // ===== Permission Management =====
    
    public PermissionDto createPermission(PermissionDto dto) {
        Permission permission = Permission.builder()
            .resourceType(dto.getResourceType())
            .operation(dto.getOperation())
            .scope(dto.getScope())
            .fieldPermissions(dto.getFieldPermissions())
            .build();
        
        Permission saved = permissionRepository.save(permission);
        logger.info("Created permission: {} {} {}", 
            saved.getResourceType(), saved.getOperation(), saved.getScope());
        
        return mapToPermissionDto(saved);
    }
    
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
            .map(this::mapToPermissionDto)
            .collect(Collectors.toList());
    }
    
    public PermissionDto getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + id + " not found"));
        return mapToPermissionDto(permission);
    }
    
    public PermissionDto updatePermission(Long id, PermissionDto dto) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + id + " not found"));
        
        permission.setResourceType(dto.getResourceType());
        permission.setOperation(dto.getOperation());
        permission.setScope(dto.getScope());
        permission.setFieldPermissions(dto.getFieldPermissions());
        
        Permission updated = permissionRepository.save(permission);
        logger.info("Updated permission: {} {} {}", 
            updated.getResourceType(), updated.getOperation(), updated.getScope());
        
        // Reload cache after permission update
        rolePermissionService.reloadCache();
        
        return mapToPermissionDto(updated);
    }
    
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + id + " not found"));
        
        // Delete all role-permission mappings
        rolePermissionRepository.deleteByPermission(permission);
        
        permissionRepository.delete(permission);
        logger.info("Deleted permission: {} {} {}", 
            permission.getResourceType(), permission.getOperation(), permission.getScope());
        
        // Reload cache after permission deletion
        rolePermissionService.reloadCache();
    }
    
    // ===== Role-Permission Assignment =====
    
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + roleId + " not found"));
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + permissionId + " not found"));
        
        if (rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
            throw new DuplicateResourceException("Role already has this permission");
        }
        
        RolePermission rolePermission = RolePermission.builder()
            .role(role)
            .permission(permission)
            .build();
        
        rolePermissionRepository.save(rolePermission);
        logger.info("Assigned permission {} to role {}", permissionId, roleId);
        
        // Reload cache after assignment
        rolePermissionService.reloadCache();
    }
    
    public void revokePermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + roleId + " not found"));
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + permissionId + " not found"));
        
        // Find and delete the role-permission mapping
        rolePermissionRepository.findAll().stream()
            .filter(rp -> rp.getRole().getId().equals(roleId) && rp.getPermission().getId().equals(permissionId))
            .findFirst()
            .ifPresent(rolePermissionRepository::delete);
        
        logger.info("Revoked permission {} from role {}", permissionId, roleId);
        
        // Reload cache after revocation
        rolePermissionService.reloadCache();
    }
    
    public List<PermissionDto> getPermissionsForRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + roleId + " not found"));
        
        return rolePermissionRepository.findPermissionsByRole(role).stream()
            .map(this::mapToPermissionDto)
            .collect(Collectors.toList());
    }
    
    // ===== User Management =====
    
    public UserDto createUser(UserDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }
        
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }
        
        Role role = roleRepository.findById(dto.getRoleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + dto.getRoleId() + " not found"));
        
        User user = User.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .department(dto.getDepartment())
            .role(role)
            .build();
        
        User saved = userRepository.save(user);
        logger.info("Created user: {}", saved.getUsername());
        
        return mapToUserDto(saved);
    }
    
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::mapToUserDto)
            .collect(Collectors.toList());
    }
    
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return mapToUserDto(user);
    }
    
    public UserDto updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        
        // Check if username is being changed to an existing username
        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }
        
        // Check if email is being changed to an existing email
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }
        
        Role role = roleRepository.findById(dto.getRoleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + dto.getRoleId() + " not found"));
        
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setDepartment(dto.getDepartment());
        user.setRole(role);
        
        User updated = userRepository.save(user);
        logger.info("Updated user: {}", updated.getUsername());
        
        return mapToUserDto(updated);
    }
    
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        
        userRepository.delete(user);
        logger.info("Deleted user: {}", user.getUsername());
    }
    
    // ===== Cache Management =====
    
    public void reloadPermissionsCache() {
        rolePermissionService.reloadCache();
        logger.info("Reloaded permissions cache");
    }
    
    // ===== Mapping Methods =====
    
    private RoleDto mapToRoleDto(Role role) {
        return new RoleDto(
            role.getId(),
            role.getName(),
            role.getHierarchyLevel(),
            role.getDescription()
        );
    }
    
    private PermissionDto mapToPermissionDto(Permission permission) {
        return new PermissionDto(
            permission.getId(),
            permission.getResourceType(),
            permission.getOperation(),
            permission.getScope(),
            permission.getFieldPermissions()
        );
    }
    
    private UserDto mapToUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDepartment(),
            user.getRole().getId(),
            user.getRole().getName()
        );
    }
}
