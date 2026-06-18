package com.demo.shared.security;

import com.demo.domain.computersystem.ComputerSystem;
import com.demo.domain.user.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JPA {@link Specification}s that restrict queries to the rows a user may access,
 * per their resolved {@link ScopeResult}. Filtering happens in-query so pagination counts
 * stay correct; the department join uses {@code distinct} to avoid duplicate rows.
 */
public final class ScopeSpecifications {

    private ScopeSpecifications() {
    }

    public static Specification<ComputerSystem> forComputerSystem(ScopeResult scope, Long userId) {
        return (root, query, cb) -> {
            if (scope.all()) {
                return cb.conjunction();
            }
            if (scope.grantsNothing()) {
                return cb.disjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> ors = new ArrayList<>();
            if (scope.own()) {
                ors.add(cb.equal(root.get("systemUser").get("id"), userId));
            }
            if (!scope.departmentIds().isEmpty()) {
                ors.add(root.join("departments").get("id").in(scope.departmentIds()));
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    public static Specification<User> forUser(ScopeResult scope, Long userId) {
        return (root, query, cb) -> {
            if (scope.all()) {
                return cb.conjunction();
            }
            if (scope.grantsNothing()) {
                return cb.disjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> ors = new ArrayList<>();
            if (scope.own()) {
                ors.add(cb.equal(root.get("id"), userId));
            }
            if (!scope.departmentIds().isEmpty()) {
                ors.add(root.join("departments").get("id").in(scope.departmentIds()));
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }
}
