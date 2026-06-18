package com.demo.domain.user;

import com.demo.domain.department.Department;
import com.demo.domain.security.role.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between User entity and UserDto.
 *
 * <p>Role and department relationships require database lookups, so {@code toEntity} and
 * {@code updateEntityFromDto} ignore them; the service layer resolves and sets them.</p>
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(target = "departmentIds", expression = "java(departmentIds(entity.getDepartments()))")
    @Mapping(target = "departmentNames", expression = "java(departmentNames(entity.getDepartments()))")
    @Mapping(target = "roleIds", expression = "java(roleIds(entity.getRoles()))")
    @Mapping(target = "roleNames", expression = "java(roleNames(entity.getRoles()))")
    UserDto toDto(User entity);

    @Mapping(target = "departments", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(UserDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "departments", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(UserDto dto, @MappingTarget User entity);

    default Set<Long> departmentIds(Set<Department> departments) {
        return departments == null ? Set.of()
                : departments.stream().map(Department::getId).collect(Collectors.toSet());
    }

    default Set<String> departmentNames(Set<Department> departments) {
        return departments == null ? Set.of()
                : departments.stream().map(Department::getName).collect(Collectors.toSet());
    }

    default Set<Long> roleIds(Set<Role> roles) {
        return roles == null ? Set.of()
                : roles.stream().map(Role::getId).collect(Collectors.toSet());
    }

    default Set<String> roleNames(Set<Role> roles) {
        return roles == null ? Set.of()
                : roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}
