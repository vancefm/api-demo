package com.demo.application.security.auth;

import com.demo.domain.security.role.Role;
import com.demo.domain.security.role.RoleDto;
import com.demo.domain.security.role.RoleMapper;
import com.demo.domain.security.permission.Permission;
import com.demo.domain.security.permission.PermissionDto;
import com.demo.domain.security.permission.PermissionMapper;
import com.demo.domain.security.rolepermission.RolePermission;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import com.demo.shared.security.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing roles and permissions.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class RoleManagementService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionService rolePermissionService;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    
    // ===== Role Management =====
    
    public RoleDto createRole(RoleDto dto) {
        if (roleRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Role with name '" + dto.getName() + "' already exists");
        }
        
        Role role = roleMapper.toEntity(dto);
        
        Role saved = roleRepository.save(role);
        log.info("Created role: {}", saved.getName());
        
        return roleMapper.toDto(saved);
    }
    
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
            .map(roleMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        return roleMapper.toDto(role);
    }
    
    public RoleDto updateRole(Long id, RoleDto dto) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        
        // Check if name is being changed to an existing name
        if (!role.getName().equals(dto.getName()) && roleRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Role with name '" + dto.getName() + "' already exists");
        }
        
        roleMapper.updateEntityFromDto(dto, role);
        
        Role updated = roleRepository.save(role);
        log.info("Updated role: {}", updated.getName());
        
        // Reload cache after role update
        rolePermissionService.reloadCache();
        
        return roleMapper.toDto(updated);
    }
    
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        
        // Delete all role-permission mappings
        rolePermissionRepository.deleteByRole(role);
        
        roleRepository.delete(role);
        log.info("Deleted role: {}", role.getName());
        
        // Reload cache after role deletion
        rolePermissionService.reloadCache();
    }

    /**
     * Resolves a Role entity by ID.
     * Used by other services (e.g., UserManagementService) to look up roles
     * while keeping role validation logic centralized.
     *
     * @param id the role ID
     * @return the Role entity
     * @throws ResourceNotFoundException if the role does not exist
     */
    public Role resolveRole(Long id) {
        return roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
    }
    
    // ===== Permission Management =====
    
    public PermissionDto createPermission(PermissionDto dto) {
        Permission permission = permissionMapper.toEntity(dto);
        
        Permission saved = permissionRepository.save(permission);
        log.info("Created permission: {} {} {}", 
            saved.getResourceType(), saved.getOperation(), saved.getScope());
        
        return permissionMapper.toDto(saved);
    }
    
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
            .map(permissionMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public PermissionDto getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + id + " not found"));
        return permissionMapper.toDto(permission);
    }
    
    public PermissionDto updatePermission(Long id, PermissionDto dto) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + id + " not found"));
        
        permissionMapper.updateEntityFromDto(dto, permission);
        
        Permission updated = permissionRepository.save(permission);
        log.info("Updated permission: {} {} {}", 
            updated.getResourceType(), updated.getOperation(), updated.getScope());
        
        // Reload cache after permission update
        rolePermissionService.reloadCache();
        
        return permissionMapper.toDto(updated);
    }
    
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + id + " not found"));
        
        // Delete all role-permission mappings
        rolePermissionRepository.deleteByPermission(permission);
        
        permissionRepository.delete(permission);
        log.info("Deleted permission: {} {} {}", 
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
        log.info("Assigned permission {} to role {}", permissionId, roleId);
        
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
        
        log.info("Revoked permission {} from role {}", permissionId, roleId);
        
        // Reload cache after revocation
        rolePermissionService.reloadCache();
    }
    
    public List<PermissionDto> getPermissionsForRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + roleId + " not found"));
        
        return rolePermissionRepository.findPermissionsByRole(role).stream()
            .map(permissionMapper::toDto)
            .collect(Collectors.toList());
    }
    
    // ===== Cache Management =====
    
    public void reloadPermissionsCache() {
        rolePermissionService.reloadCache();
        log.info("Reloaded permissions cache");
    }
}
