package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystem;
import com.demo.domain.computersystem.ComputerSystemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between ComputerSystem entity and ComputerSystemDto.
 */
@Mapper(componentModel = "spring")
public interface ComputerSystemMapper {

    ComputerSystemDto toDto(ComputerSystem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "systemUser", ignore = true)
    ComputerSystem toEntity(ComputerSystemDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "systemUser", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ComputerSystemDto dto, @MappingTarget ComputerSystem entity);
}
