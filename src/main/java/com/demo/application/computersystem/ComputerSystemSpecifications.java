package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * User-supplied filter predicates for computer systems. Composed with the authorization
 * scope {@link com.demo.shared.security.ScopeSpecifications} in the service layer.
 */
public final class ComputerSystemSpecifications {

    private ComputerSystemSpecifications() {
    }

    public static Specification<ComputerSystem> filter(String hostname, Long departmentId, Long userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hostname != null && !hostname.isBlank()) {
                predicates.add(cb.like(root.get("hostname"), "%" + hostname + "%"));
            }
            if (departmentId != null) {
                if (query != null) {
                    query.distinct(true);
                }
                predicates.add(cb.equal(root.join("departments").get("id"), departmentId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("systemUser").get("id"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
