package com.demo.application.security;

import com.demo.domain.security.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Permission entity.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * Find all permissions for a specific resource type.
     */
    List<Permission> findByResourceType(String resourceType);
    
    /**
     * Find a permission by resource type, operation, and scope.
     */
    List<Permission> findByResourceTypeAndOperationAndScope(
        String resourceType, String operation, String scope);
}
