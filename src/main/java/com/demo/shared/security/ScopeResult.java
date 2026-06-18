package com.demo.shared.security;

import java.util.Set;

/**
 * The effective row-matching scope a user has for a given resource type + operation,
 * aggregated (union) across all their roles' permissions.
 *
 * @param all          true if any matching permission is ALL-scoped (no row restriction)
 * @param own          true if any matching permission is OWN-scoped
 * @param departmentIds the user's own department ids, populated when a DEPARTMENT-scoped
 *                      permission applies; access requires the object's departments to
 *                      intersect these
 */
public record ScopeResult(boolean all, boolean own, Set<Long> departmentIds) {

    public boolean grantsNothing() {
        return !all && !own && departmentIds.isEmpty();
    }
}
