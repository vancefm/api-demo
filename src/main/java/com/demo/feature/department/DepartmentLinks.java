package com.demo.feature.department;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Helper for maintaining an owner's collection of department link entities.
 *
 * <p>Owner features map their own join entity (e.g. {@code UserDepartment}),
 * so this class stays generic over the link type: callers supply an accessor
 * for the link's department and a factory for new links.
 */
public final class DepartmentLinks {

    private DepartmentLinks() {
    }

    /**
     * Reconciles {@code links} with {@code desired}, mutating the collection in
     * place. Links whose department is no longer desired are removed; departments
     * without a link get one from {@code factory}; links that survive are left
     * untouched.
     *
     * <p>Diffing rather than clear-and-rebuild preserves the surviving links'
     * audit timestamps and avoids pointless delete/insert churn — which matters
     * once the join entity carries attributes of its own. Relies on the owner's
     * collection being mapped with {@code orphanRemoval = true} so removals are
     * flushed without an explicit repository call.
     *
     * @param links        the owner's live link collection
     * @param departmentOf reads the department out of a link
     * @param desired      the departments the owner should end up associated with
     * @param factory      builds a link for a newly associated department
     */
    public static <L> void replace(Set<L> links,
                                   Function<L, Department> departmentOf,
                                   Collection<Department> desired,
                                   Function<Department, L> factory) {
        Set<Long> desiredIds = new HashSet<>();
        for (Department department : desired) {
            desiredIds.add(department.getId());
        }

        links.removeIf(link -> !desiredIds.contains(departmentOf.apply(link).getId()));

        Set<Long> existingIds = new HashSet<>();
        for (L link : links) {
            existingIds.add(departmentOf.apply(link).getId());
        }

        for (Department department : desired) {
            if (!existingIds.contains(department.getId())) {
                links.add(factory.apply(department));
            }
        }
    }
}
