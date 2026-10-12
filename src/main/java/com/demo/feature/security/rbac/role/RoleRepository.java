package com.demo.feature.security.rbac.role;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Role}. Single-entity reads graph the permissions;
 * paged reads leave them to {@code @BatchSize} (see the fetching rule in
 * {@code UserRepository}). Permissions have no repository — they are owned by
 * the role's collection.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Override
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findById(Long id);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
