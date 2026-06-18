package com.demo.domain.computersystem;

import com.demo.domain.department.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between ComputerSystem entity and ComputerSystemDto.
 *
 * <p>Department and assigned-user relationships require database lookups, so {@code toEntity}
 * and {@code updateEntityFromDto} ignore them; the service layer resolves and sets them.</p>
 */
@Mapper(componentModel = "spring")
public interface ComputerSystemMapper {

    @Mapping(source = "systemUser.id", target = "userId")
    @Mapping(target = "departmentIds", expression = "java(departmentIds(entity.getDepartments()))")
    @Mapping(target = "departmentNames", expression = "java(departmentNames(entity.getDepartments()))")
    ComputerSystemDto toDto(ComputerSystem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "systemUser", ignore = true)
    @Mapping(target = "departments", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ComputerSystem toEntity(ComputerSystemDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "systemUser", ignore = true)
    @Mapping(target = "departments", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ComputerSystemDto dto, @MappingTarget ComputerSystem entity);

    default Set<Long> departmentIds(Set<Department> departments) {
        return departments == null ? Set.of()
                : departments.stream().map(Department::getId).collect(Collectors.toSet());
    }

    default Set<String> departmentNames(Set<Department> departments) {
        return departments == null ? Set.of()
                : departments.stream().map(Department::getName).collect(Collectors.toSet());
    }
}
