package com.demo.feature.security.rbac.access;

import com.demo.feature.computersystem.ComputerSystemDto;
import com.demo.feature.department.DepartmentDto;
import com.demo.feature.user.UserDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldDiffTest {

    private static UserDto user(String username, String email, String first, String last, List<Long> departments) {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFirstName(first);
        dto.setLastName(last);
        dto.setDepartmentIds(departments);
        return dto;
    }

    @Test
    void editableFieldsExcludeIdAndReadOnlyProperties() {
        Set<String> editable = FieldDiff.editableFields(UserDto.class);

        assertEquals(Set.of("username", "email", "firstName", "lastName", "departmentIds", "managerId"), editable);
        assertFalse(editable.contains("departments"), "@Schema(READ_ONLY)");
        assertFalse(editable.contains("roleAssignments"), "@Schema(READ_ONLY)");

        assertFalse(FieldDiff.editableFields(ComputerSystemDto.class).contains("departments"));
        assertEquals(Set.of("name", "description"), FieldDiff.editableFields(DepartmentDto.class));
    }

    @Test
    void changedFieldsReportsOnlyWhatDiffers() {
        UserDto stored = user("alice", "a@example.com", "Alice", "Smith", List.of(1L));
        UserDto incoming = user("alice", "a@example.com", "Alice", "Jones", List.of(1L));
        incoming.setDepartments(List.of(DepartmentDto.builder().id(1L).name("IT").build())); // echoed, ignored

        assertEquals(Set.of("lastName"), FieldDiff.changedFields(stored, incoming));
    }

    @Test
    void nullAndEmptyCollectionsAreTheSameValue() {
        UserDto stored = user("alice", "a@example.com", null, null, List.of());
        UserDto incoming = user("alice", "a@example.com", null, null, null);

        assertTrue(FieldDiff.changedFields(stored, incoming).isEmpty());
    }

    @Test
    void clearingDepartmentsIsAChange() {
        UserDto stored = user("alice", "a@example.com", null, null, List.of(1L));
        UserDto incoming = user("alice", "a@example.com", null, null, null);

        assertEquals(Set.of("departmentIds"), FieldDiff.changedFields(stored, incoming));
    }

    @Test
    void suppliedFieldsSkipsNullsAndEmptyCollections() {
        UserDto dto = user("alice", "a@example.com", null, "", List.of());

        assertEquals(Set.of("username", "email", "lastName"), FieldDiff.suppliedFields(dto));
    }

    @Test
    void retainUnreadableCopiesStoredValuesForFieldsTheCallerCannotSee() {
        UserDto stored = user("alice", "a@example.com", "Alice", "Smith", List.of(1L, 2L));
        stored.setManagerId(9L);
        // The caller only saw firstName/lastName and sent back a body with the
        // rest missing, changing lastName.
        UserDto incoming = new UserDto();
        incoming.setId(1L);
        incoming.setFirstName("Alice");
        incoming.setLastName("Jones");

        FieldDiff.retainUnreadable(stored, incoming, Set.of("id", "firstName", "lastName"));

        assertEquals("alice", incoming.getUsername());
        assertEquals("a@example.com", incoming.getEmail());
        assertEquals(List.of(1L, 2L), incoming.getDepartmentIds());
        assertEquals(9L, incoming.getManagerId());
        assertEquals("Jones", incoming.getLastName(), "readable fields keep the incoming value");
        assertEquals(Set.of("lastName"), FieldDiff.changedFields(stored, incoming));
    }

    @Test
    void retainUnreadableLeavesReadableFieldsAloneEvenWhenNull() {
        UserDto stored = user("alice", "a@example.com", "Alice", "Smith", List.of(1L));
        UserDto incoming = user("alice", "a@example.com", null, "Smith", List.of(1L));

        FieldDiff.retainUnreadable(stored, incoming, FieldDiff.editableFields(UserDto.class));

        assertNull(incoming.getFirstName(), "a readable field set to null is a deliberate clear");
    }
}
