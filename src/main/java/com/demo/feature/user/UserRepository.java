package com.demo.feature.user;

import com.demo.feature.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Removes all user associations for a department (cascade-dissociate on
     * department deletion). Native SQL — the join table has no entity mapping.
     */
    @Modifying
    @Query(value = "DELETE FROM user_departments WHERE department_id = :departmentId", nativeQuery = true)
    void removeDepartmentAssociations(@Param("departmentId") Long departmentId);
}
