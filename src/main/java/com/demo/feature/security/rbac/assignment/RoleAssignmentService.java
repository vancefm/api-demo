package com.demo.feature.security.rbac.assignment;
import com.demo.feature.security.rbac.role.RoleService;
import com.demo.feature.security.rbac.access.AccessControl;
import com.demo.feature.security.rbac.role.Role;
import com.demo.feature.security.rbac.role.Operation;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentService;
import com.demo.feature.user.User;
import com.demo.feature.user.UserManagementService;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Grants and revokes Department:Role pairs on users.
 *
 * <p>Cross-feature lookups go through the owning services
 * ({@link UserManagementService#resolveUser}, {@link RoleService#resolveRole},
 * {@link DepartmentService#resolveDepartment}), which own the 404 behaviour.
 *
 * <p>Governed by the {@code RoleAssignment} secured entity, whose scope is the
 * department the grant is for: a caller with CREATE/DELETE on
 * {@code RoleAssignment} in department D can hand out and revoke roles within
 * D; a global grant (no department) needs a global permission. Listing shows
 * only the grants the caller may READ.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class RoleAssignmentService {

    private static final String ROLE_ASSIGNMENT = RoleAssignmentSecuredEntity.NAME;

    private final RoleAssignmentRepository repository;
    private final RoleAssignmentMapper mapper;
    private final UserManagementService userManagementService;
    private final RoleService roleService;
    private final DepartmentService departmentService;
    private final AccessControl accessControl;

    @Transactional(readOnly = true)
    public List<RoleAssignmentDto> list(Long userId) {
        userManagementService.resolveUser(userId);
        return repository.findByUserId(userId).stream()
            .map(mapper::toDto)
            .filter(dto -> accessControl.isAllowed(ROLE_ASSIGNMENT, Operation.READ,
                accessControl.scopeOf(ROLE_ASSIGNMENT, dto)))
            .map(dto -> accessControl.filterReadable(ROLE_ASSIGNMENT, dto))
            .sorted(Comparator.comparing(RoleAssignmentDto::getId))
            .toList();
    }

    /**
     * Grants {@code dto.roleId} to the user, scoped to {@code dto.departmentId}
     * or globally when that is null. Granting the same pair twice is a 409.
     */
    public RoleAssignmentDto grant(Long userId, RoleAssignmentDto dto) {
        accessControl.requireAccess(ROLE_ASSIGNMENT, Operation.CREATE, accessControl.scopeOf(ROLE_ASSIGNMENT, dto));

        User user = userManagementService.resolveUser(userId);
        Role role = roleService.resolveRole(dto.getRoleId());
        Department department = dto.getDepartmentId() == null
            ? null
            : departmentService.resolveDepartment(dto.getDepartmentId());

        boolean exists = department == null
            ? repository.existsByUserIdAndRoleIdAndDepartmentIsNull(userId, role.getId())
            : repository.existsByUserIdAndRoleIdAndDepartmentId(userId, role.getId(), department.getId());
        if (exists) {
            throw new DuplicateResourceException("User " + user.getUsername() + " already holds role '"
                + role.getName() + "' " + scopeLabel(department));
        }

        RoleAssignment saved = repository.save(RoleAssignment.builder()
            .user(user)
            .role(role)
            .department(department)
            .build());
        // Keep the inverse side consistent within this persistence context so a
        // user loaded earlier in the same transaction reflects the new grant.
        user.getRoleAssignments().add(saved);
        log.info("Granted role '{}' to user {} {}", role.getName(), user.getUsername(), scopeLabel(department));
        return accessControl.filterReadable(ROLE_ASSIGNMENT, mapper.toDto(saved));
    }

    public void revoke(Long userId, Long assignmentId) {
        User user = userManagementService.resolveUser(userId);
        RoleAssignment assignment = repository.findByIdAndUserId(assignmentId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Role assignment with id " + assignmentId + " for user " + userId + " not found"));
        accessControl.requireAccess(ROLE_ASSIGNMENT, Operation.DELETE,
            accessControl.scopeOf(ROLE_ASSIGNMENT, mapper.toDto(assignment)));

        user.getRoleAssignments().remove(assignment);
        repository.delete(assignment);
        log.info("Revoked role assignment {} from user {}", assignmentId, userId);
    }

    private static String scopeLabel(Department department) {
        return department == null ? "globally" : "in department '" + department.getName() + "'";
    }
}
