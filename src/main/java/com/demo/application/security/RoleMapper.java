package com.demo.application.security;

import com.demo.domain.security.Role;
import com.demo.domain.security.dto.RoleDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between Role entity and RoleDto.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleDto toDto(Role entity);

    Role toEntity(RoleDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(RoleDto dto, @MappingTarget Role entity);
}
