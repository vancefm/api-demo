package com.demo.feature.computersystem;

import com.demo.feature.department.Department;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable {@link Specification}s for filtering computer systems.
 * Individual specifications assume a non-null argument; optional filters are
 * composed via {@link #withFilters}, which skips absent (null) parameters.
 */
final class ComputerSystemSpecifications {

    private ComputerSystemSpecifications() {
    }

    /**
     * Combines the optional filter criteria; a null parameter contributes no
     * predicate. With no criteria at all this matches everything.
     */
    static Specification<ComputerSystem> withFilters(String hostname, Long departmentId, Long userId) {
        List<Specification<ComputerSystem>> specs = new ArrayList<>();
        if (hostname != null) {
            specs.add(hostnameContains(hostname));
        }
        if (departmentId != null) {
            specs.add(inDepartment(departmentId));
        }
        if (userId != null) {
            specs.add(assignedToUser(userId));
        }
        return Specification.allOf(specs);
    }

    static Specification<ComputerSystem> hostnameContains(String hostname) {
        return (root, query, cb) -> cb.like(root.get("hostname"), "%" + hostname + "%");
    }

    /**
     * Membership in the given department. Uses a correlated EXISTS subquery
     * rather than a join: a join would fan out one row per associated
     * department, inflating Page totals.
     */
    static Specification<ComputerSystem> inDepartment(Long departmentId) {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<ComputerSystem> correlated = subquery.correlate(root);
            Join<ComputerSystem, Department> department = correlated.join("departments");
            subquery.select(cb.literal(1)).where(cb.equal(department.get("id"), departmentId));
            return cb.exists(subquery);
        };
    }

    static Specification<ComputerSystem> assignedToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("systemUser").get("id"), userId);
    }
}
