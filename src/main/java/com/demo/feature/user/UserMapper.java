package com.demo.feature.user;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentDto;
import com.demo.feature.department.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Maps between {@link User} entities and {@link UserDto}.
 *
 * Note: The manager and department relationships require database lookups, so
 * toEntity and updateEntityFromDto do not set them. The service layer is
 * responsible for resolving and setting those entities.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final DepartmentMapper departmentMapper;

    public UserDto toDto(User entity) {
        if (entity == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setManagerId(entity.getManager() != null ? entity.getManager().getId() : null);
        // Sorted by id for deterministic JSON output — Set iteration order is undefined.
        dto.setDepartments(departmentsOf(entity)
            .map(departmentMapper::toDto)
            .sorted(Comparator.comparing(DepartmentDto::getId))
            .toList());
        // Also populated on read so a fetched DTO can be sent back as a PUT body
        // without losing its department associations.
        dto.setDepartmentIds(departmentsOf(entity)
            .map(Department::getId)
            .sorted()
            .toList());
        return dto;
    }

    private static Stream<Department> departmentsOf(User entity) {
        return entity.getDepartmentLinks().stream()
            .map(UserDepartment::getDepartment);
    }

    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        return User.builder()
            .id(dto.getId())
            .username(dto.getUsername())
            .email(dto.getEmail())
            .build();
    }

    public void updateEntityFromDto(UserDto dto, User entity) {
        if (dto == null) {
            return;
        }

        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
    }
}
