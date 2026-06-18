package com.demo.application.user;

import com.demo.application.department.DepartmentRepository;
import com.demo.application.security.auth.RoleRepository;
import com.demo.domain.department.Department;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.User;
import com.demo.domain.user.UserDto;
import com.demo.domain.user.UserMapper;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import com.demo.shared.security.CurrentUserService;
import com.demo.shared.security.FieldProjectionService;
import com.demo.shared.security.GrantService;
import com.demo.shared.security.ScopeResult;
import com.demo.shared.security.ScopeSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing users with department/role-based access control.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserManagementService {

    private static final String RESOURCE_TYPE = "User";
    private static final String NOT_FOUND_SUFFIX = " not found";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final GrantService grantService;
    private final FieldProjectionService fieldProjectionService;
    private final PasswordEncoder passwordEncoder;

    public UserDto createUser(UserDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        User user = userMapper.toEntity(dto);
        user.setRoles(resolveRoles(dto.getRoleIds()));
        user.setDepartments(resolveDepartments(dto.getDepartmentIds()));
        if (dto.getManagerId() != null) {
            user.setManager(resolveUser(dto.getManagerId()));
        }

        User currentUser = requireAccess(user, GrantService.WRITE);

        User saved = userRepository.save(user);
        log.info("Created user: {}", saved.getUsername());
        return fieldProjectionService.filterReadable(currentUser, saved, userMapper.toDto(saved), RESOURCE_TYPE);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        User currentUser = currentUserService.requireCurrentUser();
        ScopeResult scope = grantService.resolveScope(currentUser, RESOURCE_TYPE, GrantService.READ);
        return userRepository.findAll(ScopeSpecifications.forUser(scope, currentUser.getId())).stream()
                .map(user -> fieldProjectionService.filterReadable(currentUser, user, userMapper.toDto(user), RESOURCE_TYPE))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = resolveUser(id);
        User currentUser = requireAccess(user, GrantService.READ);
        return fieldProjectionService.filterReadable(currentUser, user, userMapper.toDto(user), RESOURCE_TYPE);
    }

    public UserDto updateUser(Long id, UserDto dto) {
        User user = resolveUser(id);
        User currentUser = requireAccess(user, GrantService.WRITE);

        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        // Validate field-level write policy against the persisted state before mutating.
        fieldProjectionService.validateWritable(currentUser, user, userMapper.toDto(user), dto, RESOURCE_TYPE);

        userMapper.updateEntityFromDto(dto, user);
        user.setRoles(resolveRoles(dto.getRoleIds()));
        user.setDepartments(resolveDepartments(dto.getDepartmentIds()));
        if (dto.getManagerId() != null) {
            user.setManager(resolveUser(dto.getManagerId()));
        } else {
            user.setManager(null);
        }

        User updated = userRepository.save(user);
        log.info("Updated user: {}", updated.getUsername());
        return fieldProjectionService.filterReadable(currentUser, updated, userMapper.toDto(updated), RESOURCE_TYPE);
    }

    public void deleteUser(Long id) {
        User user = resolveUser(id);
        requireAccess(user, GrantService.DELETE);
        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    /**
     * Self-service password change for the current (DB-authenticated) user. Verifies the
     * current password; not a writable DTO field. AD users manage passwords in the directory.
     */
    public void changeOwnPassword(String currentPassword, String newPassword) {
        User current = currentUserService.requireCurrentUser();
        User user = resolveUser(current.getId());
        if (user.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AccessDeniedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    private User requireAccess(User target, String operation) {
        User currentUser = currentUserService.requireCurrentUser();
        if (!grantService.canAccess(currentUser, target, RESOURCE_TYPE, operation)) {
            throw new AccessDeniedException("Not permitted to " + operation + " this user");
        }
        return currentUser;
    }

    private User resolveUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + NOT_FOUND_SUFFIX));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        List<Role> found = roleRepository.findAllById(roleIds);
        if (found.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles not found: " + roleIds);
        }
        return new LinkedHashSet<>(found);
    }

    private Set<Department> resolveDepartments(Set<Long> departmentIds) {
        List<Department> found = departmentRepository.findAllById(departmentIds);
        if (found.size() != departmentIds.size()) {
            throw new ResourceNotFoundException("One or more departments not found: " + departmentIds);
        }
        return new LinkedHashSet<>(found);
    }
}
