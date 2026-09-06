package com.demo.feature.security.rbac.assignment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RoleAssignment}.
 *
 * <p>{@link #findByUserId} is the access-control hot path: it loads a caller's
 * grants together with each role's permissions and the scoping department in
 * one graphed query, which is everything needed to decide a request.
 */
@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, Long> {

    @EntityGraph(attributePaths = {"role", "role.permissions", "department"})
    List<RoleAssignment> findByUserId(Long userId);

    Optional<RoleAssignment> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndRoleIdAndDepartmentId(Long userId, Long roleId, Long departmentId);

    boolean existsByUserIdAndRoleIdAndDepartmentIsNull(Long userId, Long roleId);
}
