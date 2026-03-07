package com.demo.domain.security.permission;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between Permission entity and PermissionDto.
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionDto toDto(Permission entity);

    Permission toEntity(PermissionDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(PermissionDto dto, @MappingTarget Permission entity);
}
