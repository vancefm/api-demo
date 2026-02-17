package com.demo.application.security.auth;

import com.demo.domain.security.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Role entity.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find a role by its name.
     */
    Optional<Role> findByName(String name);
    
    /**
     * Check if a role with the given name exists.
     */
    boolean existsByName(String name);
}
