package com.demo.application.security;

import com.demo.domain.security.Role;
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
