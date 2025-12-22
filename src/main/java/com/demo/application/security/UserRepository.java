package com.demo.application.security;

import com.demo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find a user by username.
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check if a user with the given username exists.
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if a user with the given email exists.
     */
    boolean existsByEmail(String email);
}
