package com.demo.application.security;

import com.demo.domain.security.Permission;
import com.demo.domain.security.Role;
import com.demo.domain.security.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for RolePermission junction entity.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    /**
     * Find all permissions for a specific role.
     */
    @Query("SELECT rp.permission FROM RolePermission rp WHERE rp.role = :role")
    List<Permission> findPermissionsByRole(@Param("role") Role role);
    
    /**
     * Find all roles that have a specific permission.
     */
    @Query("SELECT rp.role FROM RolePermission rp WHERE rp.permission = :permission")
    List<Role> findRolesByPermission(@Param("permission") Permission permission);
    
    /**
     * Delete all role permissions for a specific role.
     */
    void deleteByRole(Role role);
    
    /**
     * Delete all role permissions for a specific permission.
     */
    void deleteByPermission(Permission permission);
    
    /**
     * Check if a role has a specific permission.
     */
    boolean existsByRoleAndPermission(Role role, Permission permission);
}
