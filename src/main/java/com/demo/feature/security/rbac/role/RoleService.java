package com.demo.feature.security.rbac.role;
import com.demo.feature.security.rbac.access.AccessControl;
import com.demo.feature.security.rbac.access.SecuredEntityRegistry;
import com.demo.feature.security.rbac.access.FieldDiff;

import com.demo.platform.exception.ConflictException;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.InvalidRequestException;
import com.demo.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages roles and their permissions.
 *
 * <p>Permissions are validated against {@link SecuredEntityRegistry} before
 * they are stored, so every row refers to a real entity and field. System roles
 * are read-only through this API.
 *
 * <p>Roles are governed by the {@code Role} secured entity, which is not
 * departmental: every operation here needs a <em>global</em> grant. Changing a
 * role's permissions counts as UPDATE of its {@code permissions} field.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private static final String NOT_FOUND = " not found";
    private static final String ROLE = RoleSecuredEntity.NAME;
    private static final String PERMISSIONS_FIELD = "permissions";
    private static final Set<Long> GLOBAL = Set.of();

    private final RoleRepository repository;
    private final RoleMapper mapper;
    private final SecuredEntityRegistry registry;
    private final AccessControl accessControl;

    public RoleDto createRole(RoleDto dto) {
        accessControl.requireAccess(ROLE, Operation.CREATE, GLOBAL);
        accessControl.requireFieldAccess(ROLE, Operation.CREATE, FieldDiff.suppliedFields(dto), GLOBAL);

        if (repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Role with name '" + dto.getName() + "' already exists");
        }

        Role role = mapper.toEntity(dto);
        if (dto.getPermissions() != null) {
            reconcilePermissions(role, dto.getPermissions());
        }

        Role saved = repository.saveAndFlush(role);
        log.info("Created role: {} with {} permission(s)", saved.getName(), saved.getPermissions().size());
        return accessControl.filterReadable(ROLE, mapper.toDto(saved));
    }

    /**
     * Reads need a global READ grant on {@code Role}; since roles have no
     * department there is no partial view, so listing without one is a 403
     * rather than an empty page.
     */
    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id) {
        accessControl.requireAccess(ROLE, Operation.READ, GLOBAL);
        return accessControl.filterReadable(ROLE, mapper.toDto(load(id)));
    }

    @Transactional(readOnly = true)
    public Page<RoleDto> getAllRoles(Pageable pageable) {
        accessControl.requireAccess(ROLE, Operation.READ, GLOBAL);
        return repository.findAll(pageable)
            .map(mapper::toDto)
            .map(dto -> accessControl.filterReadable(ROLE, dto));
    }

    /**
     * Updates name and description. Permissions in the body are ignored — use
     * the {@code /permissions} sub-resource. A system role keeps its name.
     */
    public RoleDto updateRole(Long id, RoleDto dto) {
        Role role = load(id);
        RoleDto stored = mapper.toDto(role);
        accessControl.requireAccess(ROLE, Operation.UPDATE, GLOBAL);
        dto.setPermissions(stored.getPermissions()); // not editable on this endpoint
        accessControl.retainUnreadable(ROLE, stored, dto, GLOBAL);
        accessControl.requireFieldAccess(ROLE, Operation.UPDATE, FieldDiff.changedFields(stored, dto), GLOBAL);

        if (!role.getName().equals(dto.getName())) {
            if (role.isSystem()) {
                throw new ConflictException("System role '" + role.getName() + "' cannot be renamed");
            }
            if (repository.existsByName(dto.getName())) {
                throw new DuplicateResourceException("Role with name '" + dto.getName() + "' already exists");
            }
        }

        mapper.updateEntityFromDto(dto, role);
        Role updated = repository.save(role);
        log.info("Updated role: {}", updated.getName());
        return accessControl.filterReadable(ROLE, mapper.toDto(updated));
    }

    /**
     * Deletes a role. Its permissions go with it (owned collection), and so do
     * any role assignments referencing it ({@code ON DELETE CASCADE}) — users who
     * held the role simply lose those grants.
     */
    public void deleteRole(Long id) {
        Role role = load(id);
        accessControl.requireAccess(ROLE, Operation.DELETE, GLOBAL);
        if (role.isSystem()) {
            throw new ConflictException("System role '" + role.getName() + "' cannot be deleted");
        }
        repository.delete(role);
        log.info("Deleted role: {}", role.getName());
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> getPermissions(Long roleId) {
        accessControl.requireFieldAccess(ROLE, Operation.READ, List.of(PERMISSIONS_FIELD), GLOBAL);
        return mapper.toDto(load(roleId)).getPermissions();
    }

    /**
     * Makes the role's permissions exactly the given list (duplicates in the
     * request collapse to one). Diffed rather than cleared and rebuilt: Hibernate
     * flushes inserts before deletes, so re-submitting an existing grant would
     * otherwise trip the unique constraint.
     */
    public List<PermissionDto> replacePermissions(Long roleId, List<PermissionDto> permissions) {
        Role role = loadEditable(roleId);
        reconcilePermissions(role, permissions);
        // The role is managed: flushing cascades the persist onto the new Permission
        // instances themselves (assigning ids). saveAndFlush would merge, which
        // persists *copies* and leaves our instances without ids.
        repository.flush();
        log.info("Replaced permissions of role {}: now {}", role.getName(), role.getPermissions().size());
        return mapper.toDto(role).getPermissions();
    }

    public PermissionDto addPermission(Long roleId, PermissionDto dto) {
        Role role = loadEditable(roleId);
        Permission requested = validated(dto);

        boolean exists = role.getPermissions().stream()
            .anyMatch(existing -> existing.key().equals(requested.key()));
        if (exists) {
            throw new DuplicateResourceException("Role '" + role.getName() + "' already has permission " + requested.key());
        }

        Permission added = role.addPermission(requested.getEntity(), requested.getField(), requested.getOperation());
        repository.flush();
        log.info("Added permission {} to role {}", added.key(), role.getName());
        return mapper.toDto(added);
    }

    public void removePermission(Long roleId, Long permissionId) {
        Role role = loadEditable(roleId);
        Permission permission = role.getPermissions().stream()
            .filter(candidate -> permissionId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "Permission with id " + permissionId + " on role " + roleId + NOT_FOUND));

        role.getPermissions().remove(permission);
        repository.flush();
        log.info("Removed permission {} from role {}", permission.key(), role.getName());
    }

    /**
     * Resolves a role by id for other features (e.g. role assignments), owning
     * the 404 behaviour.
     */
    @Transactional(readOnly = true)
    public Role resolveRole(Long id) {
        return load(id);
    }

    private Role load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + NOT_FOUND));
    }

    /**
     * Loads a role whose permissions may be changed by the caller: needs a
     * global UPDATE grant on {@code Role.permissions}, and the role must not be
     * a system role.
     */
    private Role loadEditable(Long id) {
        Role role = load(id);
        accessControl.requireFieldAccess(ROLE, Operation.UPDATE, List.of(PERMISSIONS_FIELD), GLOBAL);
        if (role.isSystem()) {
            throw new ConflictException("Permissions of system role '" + role.getName() + "' cannot be changed");
        }
        return role;
    }

    /**
     * Reconciles {@code role.permissions} with the request: grants no longer
     * requested are removed, missing ones are added, survivors are untouched.
     */
    private void reconcilePermissions(Role role, List<PermissionDto> requested) {
        Map<String, Permission> desired = new LinkedHashMap<>();
        for (PermissionDto dto : requested) {
            Permission permission = validated(dto);
            desired.putIfAbsent(permission.key(), permission);
        }

        role.getPermissions().removeIf(existing -> !desired.containsKey(existing.key()));

        role.getPermissions().stream()
            .map(Permission::key)
            .toList()
            .forEach(desired::remove);

        desired.values().stream()
            .sorted(Comparator.comparing(Permission::key))
            .forEach(p -> role.addPermission(p.getEntity(), p.getField(), p.getOperation()));
    }

    /**
     * Normalises and validates one requested permission: entity and operation
     * are required, a blank field means {@link Permission#ANY}, DELETE is always
     * entity-wide, and the target must exist in the registry.
     */
    private Permission validated(PermissionDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getEntity())) {
            throw new InvalidRequestException("Permission entity is required");
        }
        if (dto.getOperation() == null) {
            throw new InvalidRequestException("Permission operation is required");
        }

        String entity = dto.getEntity().trim();
        String field = StringUtils.hasText(dto.getField()) ? dto.getField().trim() : Permission.ANY;
        if (dto.getOperation() == Operation.DELETE && !Permission.ANY.equals(field)) {
            throw new InvalidRequestException("DELETE permissions apply to the whole entity; use field '*'");
        }

        registry.requireKnown(entity, field);

        return Permission.builder()
            .entity(entity)
            .field(field)
            .operation(dto.getOperation())
            .build();
    }
}
