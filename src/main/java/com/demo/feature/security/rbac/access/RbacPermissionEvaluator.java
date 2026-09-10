package com.demo.feature.security.rbac.access;

import com.demo.feature.security.rbac.role.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;

/**
 * Exposes {@link AccessControl} through Spring Security's standard
 * {@link PermissionEvaluator} hook, so the same rules can be written as SpEL:
 *
 * <pre>{@code
 * @PreAuthorize("hasPermission(#dto, 'UPDATE')")          // scope taken from the DTO
 * @PreAuthorize("hasPermission(#id, 'Department', 'READ')")
 * }</pre>
 *
 * <p>This adds no rules of its own — it is an adapter. The services deliberately
 * call {@link AccessControl} directly instead, because a method annotation
 * cannot express the parts that matter here: the union of current and requested
 * scope on an update, the field-level diff, or the masking of the response. Use
 * the annotation form only for a plain entity-level gate.
 */
@Component
@RequiredArgsConstructor
public class RbacPermissionEvaluator implements PermissionEvaluator {

    private final AccessControl accessControl;
    private final SecuredEntityRegistry registry;

    /**
     * {@code hasPermission(dto, 'UPDATE')} — the entity and the target scope are
     * both read from the DTO via its {@link SecuredEntity} descriptor.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (target == null) {
            return false;
        }
        return registry.findByDtoClass(target.getClass())
            .filter(secured -> accessControl.isAllowed(secured.name(), operation(permission),
                secured.departmentIds(target)))
            .isPresent();
    }

    /**
     * {@code hasPermission(id, 'Department', 'READ')} — the id is not resolved to
     * an object, so this can only answer for a caller holding a global grant.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        return registry.find(targetType)
            .filter(secured -> accessControl.isAllowed(targetType, operation(permission), Set.of()))
            .isPresent();
    }

    private static Operation operation(Object permission) {
        return Operation.valueOf(String.valueOf(permission).toUpperCase());
    }
}
