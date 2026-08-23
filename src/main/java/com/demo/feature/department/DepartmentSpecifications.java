package com.demo.feature.department;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable {@link Specification}s for filtering departments.
 * Individual specifications assume a non-null argument; optional filters are
 * composed via {@link #withFilters}, which skips absent (null) parameters.
 */
final class DepartmentSpecifications {

    private DepartmentSpecifications() {
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
