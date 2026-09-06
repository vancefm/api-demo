package com.demo.feature.security.rbac;

import com.demo.feature.department.DepartmentDto;
import com.demo.feature.user.UserDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldAccessFilterTest {

    private static UserDto fullUser() {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("alice");
        dto.setEmail("alice@example.com");
        dto.setFirstName("Alice");
        dto.setLastName("Smith");
        dto.setManagerId(9L);
        dto.setDepartmentIds(List.of(1L));
        dto.setDepartments(List.of(DepartmentDto.builder().id(1L).name("IT").build()));
        return dto;
    }

    @Test
    void nullsEverythingOutsideTheReadableSetButKeepsId() {
        UserDto dto = fullUser();

        UserDto result = FieldAccessFilter.retainOnly(dto, Set.of("firstName", "lastName"));

        assertSame(dto, result);
        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertNull(result.getUsername());
        assertNull(result.getEmail());
        assertNull(result.getManagerId());
        assertNull(result.getDepartmentIds());
        assertNull(result.getDepartments());
    }

    @Test
    void maskedFieldsAreOmittedFromJsonNotRenderedAsNull() {
        UserDto dto = FieldAccessFilter.retainOnly(fullUser(), Set.of("firstName"));

        String json = JsonMapper.builder().build().writeValueAsString(dto);

        assertTrue(json.contains("\"firstName\":\"Alice\""));
        assertTrue(json.contains("\"id\":1"));
        assertFalse(json.contains("email"), json);
        assertFalse(json.contains("username"), json);
        assertFalse(json.contains("null"), json);
    }

    @Test
    void fullReadableSetChangesNothing() {
        UserDto dto = fullUser();

        FieldAccessFilter.retainOnly(dto, Set.of("id", "username", "email", "firstName", "lastName",
            "managerId", "departmentIds", "departments", "roleAssignments"));

        assertEquals("alice", dto.getUsername());
        assertEquals(List.of(1L), dto.getDepartmentIds());
    }

    @Test
    void nullDtoIsReturnedAsIs() {
        assertNull(FieldAccessFilter.retainOnly(null, Set.of("id")));
    }
}
