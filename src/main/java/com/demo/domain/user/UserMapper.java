package com.demo.domain.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between User entity and UserDto.
 *
 * Note: The role relationship requires a database lookup, so toEntity and
 * updateEntityFromDto ignore the role field. The service layer is responsible
 * for resolving and setting the Role entity.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.id", target = "roleId")
    @Mapping(source = "role.name", target = "roleName")
    @Mapping(source = "manager.id", target = "managerId")
    UserDto toDto(User entity);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(UserDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(UserDto dto, @MappingTarget User entity);
}
