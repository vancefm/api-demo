package com.demo.feature.user;

import com.demo.feature.department.DepartmentLinks;
import com.demo.feature.department.DepartmentService;
import com.demo.feature.department.DepartmentSpecifications;
import com.demo.feature.security.rbac.AccessControl;
import com.demo.feature.security.rbac.FieldDiff;
import com.demo.feature.security.rbac.Operation;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing users.
 *
 * <p>Every API operation is guarded by {@link AccessControl} against the
 * {@code User} secured entity: a user's scope is the set of departments it
 * belongs to, so a department-scoped grant reaches only users in that
 * department, and a user in no department is reachable only through a global
 * grant. Responses are masked to the fields the caller may read; updates count
 * only fields whose value actually changes, and fields the caller cannot read
 * are ignored on write (see {@link FieldDiff}).
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserManagementService {

    private static final String USER = UserSecuredEntity.NAME;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DepartmentService departmentService;
    private final AccessControl accessControl;

    public UserDto createUser(UserDto dto) {
        Set<Long> scope = accessControl.scopeOf(USER, dto);
        accessControl.requireAccess(USER, Operation.CREATE, scope);
        accessControl.requireFieldAccess(USER, Operation.CREATE, FieldDiff.suppliedFields(dto), scope);

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        User user = userMapper.toEntity(dto);
        setDepartmentLinks(user, dto.getDepartmentIds());

        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager with id " + dto.getManagerId() + " not found"));
            user.setManager(manager);
        }

        User saved = userRepository.save(user);
        log.info("Created user: {}", saved.getUsername());

        return accessControl.filterReadable(USER, userMapper.toDto(saved));
    }

    /**
     * Pages users the caller may read: everyone for a global grant, otherwise
     * only members of the caller's readable departments (an empty page when
     * there are none).
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(readScope(), pageable)
            .map(userMapper::toDto)
            .map(dto -> accessControl.filterReadable(USER, dto));
    }

    /**
     * Filters users by username/email (partial match), department membership,
     * and/or manager; null parameters are ignored. Results are additionally
     * restricted to what the caller may read, as in {@link #getAllUsers}.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> filterUsers(String username, String email, Long departmentId, Long managerId,
                                     Pageable pageable) {
        Specification<User> filters = UserSpecifications.withFilters(username, email, departmentId, managerId);
        return userRepository
            .findAll(filters.and(readScope()), pageable)
            .map(userMapper::toDto)
            .map(dto -> accessControl.filterReadable(USER, dto));
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        UserDto dto = userMapper.toDto(load(id));
        accessControl.requireAccess(USER, Operation.READ, accessControl.scopeOf(USER, dto));
        return accessControl.filterReadable(USER, dto);
    }

    public UserDto updateUser(Long id, UserDto dto) {
        User user = load(id);
        UserDto stored = userMapper.toDto(user);

        // The caller must be allowed to update the user where it is now *and*
        // where the request puts it.
        Set<Long> scope = new HashSet<>(accessControl.scopeOf(USER, stored));
        scope.addAll(accessControl.scopeOf(USER, dto));
        accessControl.requireAccess(USER, Operation.UPDATE, scope);

        // Fields the caller never saw are not theirs to change: keep stored values.
        accessControl.retainUnreadable(USER, stored, dto, scope);
        accessControl.requireFieldAccess(USER, Operation.UPDATE, FieldDiff.changedFields(stored, dto), scope);

        // Check if username is being changed to an existing username
        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }

        // Check if email is being changed to an existing email
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        userMapper.updateEntityFromDto(dto, user);
        setDepartmentLinks(user, dto.getDepartmentIds());

        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager with id " + dto.getManagerId() + " not found"));
            user.setManager(manager);
        } else {
            user.setManager(null);
        }

        User updated = userRepository.save(user);
        log.info("Updated user: {}", updated.getUsername());

        return accessControl.filterReadable(USER, userMapper.toDto(updated));
    }

    /**
     * Deletes a user. Its department links and role assignments are removed by
     * the {@code ON DELETE CASCADE} foreign keys.
     */
    public void deleteUser(Long id) {
        User user = load(id);
        accessControl.requireAccess(USER, Operation.DELETE, accessControl.scopeOf(USER, userMapper.toDto(user)));

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    /**
     * Resolves a user by id for other features (e.g. role assignments), owning
     * the 404 behaviour — the counterpart of
     * {@link DepartmentService#resolveDepartments}. Not access-checked: the
     * calling feature enforces its own entity's permissions.
     */
    @Transactional(readOnly = true)
    public User resolveUser(Long id) {
        return load(id);
    }

    /**
     * Returns the user with this username, creating it if it does not exist yet.
     *
     * <p>Called by the authentication layer after a successful directory bind:
     * the directory proves identity, this table holds the profile and role
     * assignments, and username is the link between them. A freshly provisioned
     * user has no role assignments and therefore no permissions. Not an API
     * operation — nothing a client sends reaches this method — so it is not
     * access-checked.
     */
    public User findOrProvision(String username, String email, String firstName, String lastName) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = User.builder()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();
            User saved = userRepository.save(user);
            log.info("Provisioned user '{}' on first directory login", saved.getUsername());
            return saved;
        });
    }

    private User load(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    /**
     * The department restriction for paged reads: nothing for a global READ
     * grant, otherwise membership in one of the caller's readable departments.
     */
    private Specification<User> readScope() {
        return accessControl.readableDepartments(USER)
            .<Specification<User>>map(DepartmentSpecifications::assignedToAnyDepartment)
            .orElseGet(Specification::unrestricted);
    }

    /**
     * Reconciles the user's department links with the requested IDs. Unknown IDs
     * fail the call with a 404 via
     * {@link DepartmentService#resolveDepartments}; null or empty clears them.
     *
     * <p>The links are owned by the {@code departmentLinks} collection
     * ({@code cascade = ALL}, {@code orphanRemoval = true}), so no repository
     * call is needed — the diff is flushed with the user itself.
     */
    private void setDepartmentLinks(User user, List<Long> departmentIds) {
        DepartmentLinks.replace(
            user.getDepartmentLinks(),
            UserDepartment::getDepartment,
            departmentService.resolveDepartments(departmentIds),
            department -> UserDepartment.builder()
                .user(user)
                .department(department)
                .build());
    }
}
