package com.demo.feature.security.rbac;

import com.demo.feature.user.UserDto;
import com.demo.platform.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecuredEntityRegistryTest {

    private final SecuredEntity<UserDto> user =
        SecuredEntity.departmental("User", UserDto.class, UserDto::getDepartmentIds);
    private final SecuredEntity<RoleDto> role = SecuredEntity.global("Role", RoleDto.class);
    private final SecuredEntityRegistry registry = new SecuredEntityRegistry(List.of(user, role));

    @Test
    void fieldNamesAreTheDtoProperties() {
        Set<String> fields = user.fieldNames();

        assertTrue(fields.containsAll(Set.of("id", "username", "email", "firstName", "lastName",
            "departmentIds", "departments", "managerId")));
        assertTrue(fields.stream().noneMatch("class"::equals));
    }

    @Test
    void departmentalScopeComesFromDepartmentIds() {
        UserDto dto = new UserDto();
        dto.setDepartmentIds(List.of(3L, 5L, 3L));

        assertEquals(Set.of(3L, 5L), user.departmentIds(dto));
    }

    @Test
    void nullDepartmentIdsMeansNoScope() {
        assertEquals(Set.of(), user.departmentIds(new UserDto()));
        assertEquals(Set.of(), role.departmentIds(new RoleDto()));
    }

    @Test
    void knownEntityAndFieldPass() {
        assertDoesNotThrow(() -> registry.requireKnown("User", "firstName"));
        assertDoesNotThrow(() -> registry.requireKnown("User", "*"));
        assertDoesNotThrow(() -> registry.requireKnown("*", "*"));
    }

    @Test
    void unknownEntityIsRejectedNamingTheKnownOnes() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
            () -> registry.requireKnown("Widget", "*"));

        assertTrue(ex.getMessage().contains("Widget"));
        assertTrue(ex.getMessage().contains("User"));
    }

    @Test
    void unknownFieldIsRejectedNamingTheKnownOnes() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
            () -> registry.requireKnown("User", "password"));

        assertTrue(ex.getMessage().contains("password"));
        assertTrue(ex.getMessage().contains("firstName"));
    }

    @Test
    void wildcardEntityRequiresWildcardField() {
        assertThrows(InvalidRequestException.class, () -> registry.requireKnown("*", "firstName"));
    }

    @Test
    void duplicateEntityNamesFailFast() {
        SecuredEntity<RoleDto> again = SecuredEntity.global("Role", RoleDto.class);

        assertThrows(IllegalStateException.class, () -> new SecuredEntityRegistry(List.of(role, again)));
    }
}
