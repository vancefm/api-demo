package com.demo.feature.security.rbac.access;

import com.demo.feature.security.rbac.role.Operation;
import com.demo.feature.security.rbac.role.RoleDto;
import com.demo.feature.user.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The adapter that exposes {@link AccessControl} as Spring Security's
 * {@code hasPermission(...)} SpEL hook. It adds no rules; it only resolves the
 * entity and scope from the argument and delegates.
 */
@ExtendWith(MockitoExtension.class)
class RbacPermissionEvaluatorTest {

    @Mock
    private AccessControl accessControl;

    private RbacPermissionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        SecuredEntityRegistry registry = new SecuredEntityRegistry(List.of(
            SecuredEntity.departmental("User", UserDto.class, UserDto::getDepartmentIds),
            SecuredEntity.global("Role", RoleDto.class)));
        evaluator = new RbacPermissionEvaluator(accessControl, registry);
    }

    private static UserDto userIn(Long... departmentIds) {
        UserDto dto = new UserDto();
        dto.setDepartmentIds(List.of(departmentIds));
        return dto;
    }

    @Test
    void objectFormResolvesEntityAndScopeFromTheDto() {
        when(accessControl.isAllowed("User", Operation.UPDATE, Set.of(3L))).thenReturn(true);

        assertTrue(evaluator.hasPermission(null, userIn(3L), "UPDATE"));
        verify(accessControl).isAllowed("User", Operation.UPDATE, Set.of(3L));
    }

    @Test
    void objectFormDeniesWhenAccessControlDoes() {
        when(accessControl.isAllowed(eq("User"), any(), any())).thenReturn(false);

        assertFalse(evaluator.hasPermission(null, userIn(3L), "UPDATE"));
    }

    @Test
    void permissionNameIsCaseInsensitive() {
        when(accessControl.isAllowed("Role", Operation.READ, Set.of())).thenReturn(true);

        assertTrue(evaluator.hasPermission(null, new RoleDto(), "read"));
    }

    @Test
    void unknownDtoTypeIsDeniedWithoutConsultingAccessControl() {
        assertFalse(evaluator.hasPermission(null, "not a secured dto", "READ"));
        assertFalse(evaluator.hasPermission(null, null, "READ"));

        verify(accessControl, never()).isAllowed(any(), any(), any());
    }

    @Test
    void idFormAsksForAGlobalGrantSinceTheObjectIsNotResolved() {
        lenient().when(accessControl.isAllowed("Department", Operation.READ, Set.of())).thenReturn(true);

        // "Department" is not registered in this test's registry
        assertFalse(evaluator.hasPermission(null, 1L, "Department", "READ"));

        when(accessControl.isAllowed("Role", Operation.DELETE, Set.of())).thenReturn(true);
        assertTrue(evaluator.hasPermission(null, 1L, "Role", "DELETE"));
    }
}
