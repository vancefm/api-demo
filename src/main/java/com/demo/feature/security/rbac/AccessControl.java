package com.demo.feature.security.rbac;

import com.demo.feature.security.auth.CurrentUser;
import com.demo.feature.security.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The decision API services call to enforce RBAC.
 *
 * <p>Every method resolves the caller through {@link CurrentUser}, loads their
 * grants (once per request — the {@link EffectivePermissions} snapshot is cached
 * as a request attribute) and evaluates the rules documented on
 * {@link EffectivePermissions}. Denials throw Spring Security's
 * {@link AccessDeniedException}, which {@code GlobalExceptionHandler} maps to a
 * 403 problem detail.
 *
 * <p>Target departments come from the DTO through the entity's
 * {@link SecuredEntity} descriptor, so services pass DTOs, not department lists,
 * wherever they can.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AccessControl {

    private static final String ID = "id";
    private static final String CACHE_KEY = AccessControl.class.getName() + ".effective";

    private final CurrentUser currentUser;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final SecuredEntityRegistry registry;

    /**
     * Departments the given DTO belongs to, per its secured-entity descriptor.
     */
    public Set<Long> scopeOf(String entity, Object dto) {
        return registry.require(entity).departmentIds(dto);
    }

    /**
     * Non-throwing form of {@link #requireAccess}, for filtering mixed-scope
     * collections item by item.
     */
    public boolean isAllowed(String entity, Operation op, Set<Long> targetDepartments) {
        return effective().allows(entity, op, targetDepartments);
    }

    /**
     * Fails with 403 unless the caller may perform {@code op} on {@code entity}
     * objects in {@code targetDepartments} (empty = needs a global grant).
     */
    public void requireAccess(String entity, Operation op, Set<Long> targetDepartments) {
        if (!effective().allows(entity, op, targetDepartments)) {
            throw denied("Not permitted to " + op + " " + entity + scopeSuffix(targetDepartments));
        }
    }

    /**
     * Fails with 403 unless every field is covered by a grant for {@code op}.
     * The message names the offending fields so a client knows what to drop.
     */
    public void requireFieldAccess(String entity, Operation op, Collection<String> fields,
                                   Set<Long> targetDepartments) {
        EffectivePermissions permissions = effective();
        Set<String> deniedFields = new TreeSet<>();
        for (String field : fields) {
            if (!permissions.allowsField(entity, field, op, targetDepartments)) {
                deniedFields.add(field);
            }
        }
        if (!deniedFields.isEmpty()) {
            throw denied("Not permitted to " + op + " " + entity + " field(s) " + deniedFields
                + scopeSuffix(targetDepartments));
        }
    }

    /**
     * Where the caller may read {@code entity}: empty Optional = everywhere.
     */
    public Optional<Set<Long>> readableDepartments(String entity) {
        return effective().readableDepartments(entity);
    }

    /**
     * Fields of {@code entity} the caller may read for objects in
     * {@code targetDepartments}; {@code id} is always included.
     */
    public Set<String> readableFields(String entity, Set<Long> targetDepartments) {
        EffectivePermissions permissions = effective();
        Set<String> readable = new LinkedHashSet<>();
        readable.add(ID);
        for (String field : registry.require(entity).fieldNames()) {
            if (permissions.allowsField(entity, field, Operation.READ, targetDepartments)) {
                readable.add(field);
            }
        }
        return readable;
    }

    /**
     * Masks the fields of {@code dto} the caller may not read (they are omitted
     * from the JSON). Scope is taken from the DTO itself.
     */
    public <T> T filterReadable(String entity, T dto) {
        return FieldAccessFilter.retainOnly(dto, readableFields(entity, scopeOf(entity, dto)));
    }

    /**
     * Before diffing an update: copies the stored value of every field the
     * caller cannot read into {@code incoming}, so unseen fields never register
     * as changes (see {@link FieldDiff#retainUnreadable}).
     */
    public <T> void retainUnreadable(String entity, T stored, T incoming, Set<Long> targetDepartments) {
        FieldDiff.retainUnreadable(stored, incoming, readableFields(entity, targetDepartments));
    }

    private EffectivePermissions effective() {
        UserPrincipal principal = currentUser.require();

        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null
            && request.getAttribute(CACHE_KEY, RequestAttributes.SCOPE_REQUEST) instanceof EffectivePermissions cached) {
            return cached;
        }

        EffectivePermissions permissions = EffectivePermissions.of(roleAssignmentRepository.findByUserId(principal.id()));
        if (request != null) {
            request.setAttribute(CACHE_KEY, permissions, RequestAttributes.SCOPE_REQUEST);
        }
        return permissions;
    }

    private AccessDeniedException denied(String message) {
        String username = currentUser.get().map(UserPrincipal::username).orElse("?");
        log.debug("Access denied for {}: {}", username, message);
        return new AccessDeniedException(message);
    }

    private static String scopeSuffix(Set<Long> targetDepartments) {
        return targetDepartments.isEmpty()
            ? " (requires a global grant)"
            : " in department(s) " + new TreeSet<>(targetDepartments);
    }
}
