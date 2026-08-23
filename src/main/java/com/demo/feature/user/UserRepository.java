package com.demo.feature.user;

import com.demo.feature.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity. Dynamic filtering is done through
 * {@link JpaSpecificationExecutor} with the specifications defined in
 * {@link UserSpecifications} — prefer composing specifications over adding
 * new {@code @Query} filter methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
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
     * department deletion). Deliberately a native {@code @Modifying} query
     * rather than a Specification or entity-level removal: the join table has
     * no entity mapping (so JPQL/Criteria cannot target it), and loading every
     * affected user just to mutate its collection would be a bulk N+1. Callers
     * must not have affected entities loaded in the persistence context (the
     * bulk delete bypasses it).
     */
    @Modifying
    @Query(value = "DELETE FROM user_departments WHERE department_id = :departmentId", nativeQuery = true)
    void removeDepartmentAssociations(@Param("departmentId") Long departmentId);
}
