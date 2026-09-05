package com.demo.feature.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity. Dynamic filtering is done through
 * {@link JpaSpecificationExecutor} with the specifications defined in
 * {@link UserSpecifications} — prefer composing specifications over adding
 * new {@code @Query} filter methods.
 *
 * <p>Association fetching follows one rule: {@code @EntityGraph} on
 * single-entity reads, {@code @BatchSize} (declared on the collection itself)
 * for paged reads. A collection-fetching entity graph combined with pagination
 * makes Hibernate join the collection and paginate in memory (HHH000104),
 * loading the entire result set — so paged methods are deliberately left
 * ungraphed for {@code departmentLinks}.
 *
 * <p>Deleting a department does not need anything here: the join entity's
 * foreign keys are declared {@code ON DELETE CASCADE}, so the database removes
 * the links.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Override
    @EntityGraph(attributePaths = {"departmentLinks", "departmentLinks.department"})
    Optional<User> findById(Long id);

    /**
     * Find a user by username.
     */
    @EntityGraph(attributePaths = {"departmentLinks", "departmentLinks.department"})
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
