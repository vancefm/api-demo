package com.demo.feature.computersystem;

import org.springframework.stereotype.Component;

/**
 * Maps between {@link ComputerSystem} entities and {@link ComputerSystemDto}.
 *
 * Note: The systemUser and createdBy relationships require a database lookup,
 * so toEntity and updateEntityFromDto do not set them. The service layer is
 * responsible for resolving and setting those entities.
 */
@Component
public class ComputerSystemMapper {

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
            .department(entity.getDepartment())
            .macAddress(entity.getMacAddress())
            .ipAddress(entity.getIpAddress())
            .networkName(entity.getNetworkName())
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
            .department(dto.getDepartment())
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
        entity.setDepartment(dto.getDepartment());
        entity.setMacAddress(dto.getMacAddress());
        entity.setIpAddress(dto.getIpAddress());
        entity.setNetworkName(dto.getNetworkName());
    }
}
