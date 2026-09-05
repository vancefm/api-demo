package com.demo.feature.user;

import com.demo.feature.department.DepartmentSpecifications;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable {@link Specification}s for filtering users.
 * Individual specifications assume a non-null argument; optional filters are
 * composed via {@link #withFilters}, which skips absent (null) parameters.
 */
final class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * Combines the optional filter criteria; a null parameter contributes no
     * predicate. With no criteria at all this matches everything.
     */
    static Specification<User> withFilters(String username, String email, Long departmentId, Long managerId) {
        List<Specification<User>> specs = new ArrayList<>();
        if (username != null) {
            specs.add(usernameContains(username));
        }
        if (email != null) {
            specs.add(emailContains(email));
        }
        if (departmentId != null) {
            specs.add(DepartmentSpecifications.assignedToDepartment(departmentId));
        }
        if (managerId != null) {
            specs.add(managedBy(managerId));
        }
        return Specification.allOf(specs);
    }

    static Specification<User> usernameContains(String username) {
        return (root, query, cb) -> cb.like(root.get("username"), "%" + username + "%");
    }

    static Specification<User> emailContains(String email) {
        return (root, query, cb) -> cb.like(root.get("email"), "%" + email + "%");
    }

    static Specification<User> managedBy(Long managerId) {
        return (root, query, cb) -> cb.equal(root.get("manager").get("id"), managerId);
    }
}
