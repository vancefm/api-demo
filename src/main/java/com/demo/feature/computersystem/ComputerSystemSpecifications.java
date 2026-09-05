package com.demo.feature.computersystem;

import com.demo.feature.department.DepartmentSpecifications;
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
            specs.add(DepartmentSpecifications.assignedToDepartment(departmentId));
        }
        if (userId != null) {
            specs.add(assignedToUser(userId));
        }
        return Specification.allOf(specs);
    }

    static Specification<ComputerSystem> hostnameContains(String hostname) {
        return (root, query, cb) -> cb.like(root.get("hostname"), "%" + hostname + "%");
    }

    static Specification<ComputerSystem> assignedToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("systemUser").get("id"), userId);
    }
}
