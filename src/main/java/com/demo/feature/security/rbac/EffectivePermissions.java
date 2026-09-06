package com.demo.feature.security.rbac;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A caller's grants flattened into something that can answer "may I?".
 *
 * <p>Built once per request from the caller's {@link RoleAssignment}s: every
 * permission of every assigned role becomes a {@link Grant} tagged with the
 * assignment's department (or {@code null} for a global grant). Pure and
 * immutable, so the decision rules are unit-testable without Spring or a
 * database.
 *
 * <h2>Decision rules</h2>
 * <ul>
 *   <li>A grant applies to a target if its entity matches (or is {@code *}) and
 *       its scope matches: a global grant always does; a department-scoped grant
 *       does when its department is one of the target's departments.</li>
 *   <li>A target with <em>no</em> departments (a role, a user in no department,
 *       …) can therefore only be reached by a global grant.</li>
 *   <li>Entity-level access to an operation needs at least one applicable grant
 *       for that operation — a field-level grant is enough (someone who may read
 *       {@code User.firstName} may read users, and sees only that field).</li>
 *   <li>Field-level access additionally needs the grant's field to match (or be
 *       {@code *}).</li>
 * </ul>
 */
public final class EffectivePermissions {

    /**
     * One permission as held by the caller: the permission's target plus the
     * department the assignment scoped it to ({@code null} = global).
     */
    public record Grant(String entity, String field, Operation operation, Long departmentId) {

        boolean isGlobal() {
            return departmentId == null;
        }

        boolean matchesEntity(String targetEntity) {
            return Permission.ANY.equals(entity) || entity.equals(targetEntity);
        }

        boolean appliesTo(String targetEntity, Operation op, Set<Long> targetDepartments) {
            return operation == op
                && matchesEntity(targetEntity)
                && (isGlobal() || targetDepartments.contains(departmentId));
        }

        boolean coversField(String fieldName) {
            return Permission.ANY.equals(field) || field.equals(fieldName);
        }
    }

    private static final EffectivePermissions NONE = new EffectivePermissions(List.of());

    private final List<Grant> grants;

    private EffectivePermissions(List<Grant> grants) {
        this.grants = List.copyOf(grants);
    }

    public static EffectivePermissions of(Collection<RoleAssignment> assignments) {
        List<Grant> grants = assignments.stream()
            .flatMap(assignment -> assignment.getRole().getPermissions().stream()
                .map(permission -> new Grant(
                    permission.getEntity(),
                    permission.getField(),
                    permission.getOperation(),
                    assignment.getDepartment() == null ? null : assignment.getDepartment().getId())))
            .toList();
        return grants.isEmpty() ? NONE : new EffectivePermissions(grants);
    }

    public static EffectivePermissions none() {
        return NONE;
    }

    public boolean isEmpty() {
        return grants.isEmpty();
    }

    /**
     * May the caller perform {@code op} on {@code entity} objects belonging to
     * {@code targetDepartments} (empty = a non-departmental object)?
     */
    public boolean allows(String entity, Operation op, Set<Long> targetDepartments) {
        return grants.stream().anyMatch(grant -> grant.appliesTo(entity, op, targetDepartments));
    }

    /**
     * Like {@link #allows} but for one field of the entity.
     */
    public boolean allowsField(String entity, String field, Operation op, Set<Long> targetDepartments) {
        return grants.stream().anyMatch(grant ->
            grant.appliesTo(entity, op, targetDepartments) && grant.coversField(field));
    }

    /**
     * Departments in which the caller may READ {@code entity} at all.
     * {@link Optional#empty()} means unrestricted (a global grant exists); an
     * empty set means nowhere.
     */
    public Optional<Set<Long>> readableDepartments(String entity) {
        Set<Long> departments = new HashSet<>();
        for (Grant grant : grants) {
            if (grant.operation() != Operation.READ || !grant.matchesEntity(entity)) {
                continue;
            }
            if (grant.isGlobal()) {
                return Optional.empty();
            }
            departments.add(grant.departmentId());
        }
        return Optional.of(Set.copyOf(departments));
    }
}
