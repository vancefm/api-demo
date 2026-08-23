package com.demo.feature.computersystem;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentDto;
import com.demo.feature.department.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Maps between {@link ComputerSystem} entities and {@link ComputerSystemDto}.
 *
 * Note: The systemUser, createdBy, and department relationships require
 * database lookups, so toEntity and updateEntityFromDto do not set them. The
 * service layer is responsible for resolving and setting those entities.
 */
@Component
@RequiredArgsConstructor
public class ComputerSystemMapper {

    private final DepartmentMapper departmentMapper;

    public ComputerSystemDto toDto(ComputerSystem entity) {
        if (entity == null) {
            return null;
        }

        return ComputerSystemDto.builder()
            .id(entity.getId())
            .hostname(entity.getHostname())
            .manufacturer(entity.getManufacturer())
            .model(entity.getModel())
            .userId(entity.getSystemUser() != null ? entity.getSystemUser().getId() : null)
            .macAddress(entity.getMacAddress())
            .ipAddress(entity.getIpAddress())
            .networkName(entity.getNetworkName())
            // Sorted by id for deterministic JSON output — Set iteration order is undefined.
            .departments(entity.getDepartments().stream()
                .map(departmentMapper::toDto)
                .sorted(Comparator.comparing(DepartmentDto::getId))
                .toList())
            // Also populated on read so a fetched DTO can be sent back as a PUT
            // body without losing its department associations.
            .departmentIds(entity.getDepartments().stream()
                .map(Department::getId)
                .sorted()
                .toList())
            .build();
    }

    public ComputerSystem toEntity(ComputerSystemDto dto) {
        if (dto == null) {
            return null;
        }

        return ComputerSystem.builder()
            .hostname(dto.getHostname())
            .manufacturer(dto.getManufacturer())
            .model(dto.getModel())
            .macAddress(dto.getMacAddress())
            .ipAddress(dto.getIpAddress())
            .networkName(dto.getNetworkName())
            .build();
    }

    public void updateEntityFromDto(ComputerSystemDto dto, ComputerSystem entity) {
        if (dto == null) {
            return;
        }

        entity.setHostname(dto.getHostname());
        entity.setManufacturer(dto.getManufacturer());
        entity.setModel(dto.getModel());
        entity.setMacAddress(dto.getMacAddress());
        entity.setIpAddress(dto.getIpAddress());
        entity.setNetworkName(dto.getNetworkName());
    }
}
