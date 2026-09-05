package com.demo.feature.department;

import org.springframework.stereotype.Component;

/**
 * Maps between {@link Department} entities and {@link DepartmentDto}.
 */
@Component
public class DepartmentMapper {

    public DepartmentDto toDto(Department entity) {
        if (entity == null) {
            return null;
        }

        return DepartmentDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .build();
    }

    public Department toEntity(DepartmentDto dto) {
        if (dto == null) {
            return null;
        }

        return Department.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .build();
    }

    public void updateEntityFromDto(DepartmentDto dto, Department entity) {
        if (dto == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
    }
}
