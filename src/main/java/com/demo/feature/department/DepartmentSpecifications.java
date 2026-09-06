package com.demo.feature.department;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reusable {@link Specification}s for filtering departments, plus
 * {@link #assignedToDepartment} for filtering any owner entity by department
 * membership.
 * Individual specifications assume a non-null argument; optional filters are
 * composed via {@link #withFilters}, which skips absent (null) parameters.
 */
public final class DepartmentSpecifications {

    /**
     * Name every owner entity's department link collection this, so
     * {@link #assignedToDepartment} works across features.
     */
    private static final String LINKS = "departmentLinks";

    private DepartmentSpecifications() {
    }

    /**
     * Membership in the given department, for any owner entity whose department
     * links are mapped as {@value #LINKS}.
     *
     * <p>Uses a correlated EXISTS subquery rather than a join: a join would fan
     * out one row per associated department, inflating Page totals.
     */
    public static <T> Specification<T> assignedToDepartment(Long departmentId) {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<T> correlated = subquery.correlate(root);
            Join<Object, Object> link = correlated.join(LINKS);
            subquery.select(cb.literal(1))
                .where(cb.equal(link.get("department").get("id"), departmentId));
            return cb.exists(subquery);
        };
    }

    /**
     * Membership in <em>any</em> of the given departments, for any owner entity
     * whose department links are mapped as {@value #LINKS}. Used by the RBAC
     * layer to restrict paged reads to the departments a caller may read.
     *
     * <p>An empty collection matches nothing (a caller with no readable
     * departments sees an empty page). Same EXISTS shape as
     * {@link #assignedToDepartment}, so an owner in several of the departments
     * is still counted once.
     */
    public static <T> Specification<T> assignedToAnyDepartment(Collection<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<T> correlated = subquery.correlate(root);
            Join<Object, Object> link = correlated.join(LINKS);
            subquery.select(cb.literal(1))
                .where(link.get("department").get("id").in(departmentIds));
            return cb.exists(subquery);
        };
    }

    /**
     * Departments whose own id is in the set — the department entity's version
     * of a read scope (a department <em>is</em> its own scope). Empty matches
     * nothing.
     */
    static Specification<Department> idIn(Collection<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("id").in(departmentIds);
    }

    /**
     * Combines the optional filter criteria; a null parameter contributes no
     * predicate. With no criteria at all this matches everything.
     */
    static Specification<Department> withFilters(String name, String description) {
        List<Specification<Department>> specs = new ArrayList<>();
        if (name != null) {
            specs.add(nameContains(name));
        }
        if (description != null) {
            specs.add(descriptionContains(description));
        }
        return Specification.allOf(specs);
    }

    static Specification<Department> nameContains(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    static Specification<Department> descriptionContains(String description) {
        return (root, query, cb) -> cb.like(root.get("description"), "%" + description + "%");
    }
}
