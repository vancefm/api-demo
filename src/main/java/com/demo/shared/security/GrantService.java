package com.demo.shared.security;

import com.demo.domain.ScopedResource;
import com.demo.domain.security.permission.Permission;
import com.demo.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central authorization evaluator. Aggregates a user's role permissions to decide
 * object-level access (OWN / DEPARTMENT / ALL scope) and field-level read/write sets.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GrantService {

    public static final String READ = "READ";
    public static final String WRITE = "WRITE";
    public static final String DELETE = "DELETE";

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_DEPARTMENT = "DEPARTMENT";
    private static final String SCOPE_OWN = "OWN";
    private static final String LEVEL_HIDDEN = "HIDDEN";
    private static final String LEVEL_WRITE = "WRITE";

    private final RolePermissionService rolePermissionService;
    private final ObjectMapper objectMapper;

    /** All of the user's permissions for a resource type + operation, across every role. */
    private List<Permission> permissionsFor(User user, String resourceType, String operation) {
        return user.getRoles().stream()
                .flatMap(role -> rolePermissionService.getPermissionsForRole(role.getName()).stream())
                .filter(p -> p.getResourceType().equals(resourceType) && p.getOperation().equals(operation))
                .toList();
    }

    /** Effective scope (union across roles) for list/collection queries. */
    public ScopeResult resolveScope(User user, String resourceType, String operation) {
        boolean all = false;
        boolean own = false;
        boolean department = false;
        for (Permission p : permissionsFor(user, resourceType, operation)) {
            switch (p.getScope()) {
                case SCOPE_ALL -> all = true;
                case SCOPE_DEPARTMENT -> department = true;
                case SCOPE_OWN -> own = true;
                default -> log.warn("Unknown permission scope '{}' on {}", p.getScope(), resourceType);
            }
        }
        Set<Long> departmentIds = department ? user.getDepartmentIds() : Collections.emptySet();
        return new ScopeResult(all, own, departmentIds);
    }

    /** Object-level check for a single resource. */
    public boolean canAccess(User user, ScopedResource resource, String resourceType, String operation) {
        for (Permission p : permissionsFor(user, resourceType, operation)) {
            if (scopeMatches(p.getScope(), user, resource)) {
                return true;
            }
        }
        return false;
    }

    private boolean scopeMatches(String scope, User user, ScopedResource resource) {
        return switch (scope) {
            case SCOPE_ALL -> true;
            case SCOPE_OWN -> resource.getOwnerId() != null && resource.getOwnerId().equals(user.getId());
            case SCOPE_DEPARTMENT -> intersects(resource.getDepartmentIds(), user.getDepartmentIds());
            default -> false;
        };
    }

    /**
     * Fields the user may read on this specific resource. {@code Optional.empty()} means
     * "no restriction" (all fields); otherwise the returned set is the whitelist.
     */
    public Optional<Set<String>> readableFields(User user, ScopedResource resource, String resourceType) {
        return fields(matchingPermissions(user, resource, resourceType, READ), false);
    }

    /** Fields the user may write on this specific resource (see {@link #readableFields}). */
    public Optional<Set<String>> writableFields(User user, ScopedResource resource, String resourceType) {
        return fields(matchingPermissions(user, resource, resourceType, WRITE), true);
    }

    private List<Permission> matchingPermissions(User user, ScopedResource resource,
                                                 String resourceType, String operation) {
        return permissionsFor(user, resourceType, operation).stream()
                .filter(p -> scopeMatches(p.getScope(), user, resource))
                .toList();
    }

    private Optional<Set<String>> fields(List<Permission> permissions, boolean writeMode) {
        Set<String> allowed = new HashSet<>();
        for (Permission p : permissions) {
            Map<String, String> fieldPerms = parseFieldPermissions(p.getFieldPermissions());
            if (fieldPerms.isEmpty()) {
                // An empty field policy grants the full field set; union short-circuits to "all".
                return Optional.empty();
            }
            fieldPerms.forEach((field, level) -> {
                if (writeMode ? LEVEL_WRITE.equals(level) : !LEVEL_HIDDEN.equals(level)) {
                    allowed.add(field);
                }
            });
        }
        return Optional.of(allowed);
    }

    private Map<String, String> parseFieldPermissions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse field permissions JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static boolean intersects(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        Set<Long> smaller = a.size() <= b.size() ? a : b;
        Set<Long> larger = smaller == a ? b : a;
        for (Long id : smaller) {
            if (larger.contains(id)) {
                return true;
            }
        }
        return false;
    }
}
