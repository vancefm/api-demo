package com.demo.feature.security.rbac;

import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Maps between {@link Role}/{@link Permission} entities and their DTOs.
 * Permissions are not mapped inbound here: {@code RoleService} validates each
 * one against the secured-entity registry before adding it.
 */
@Component
public class RoleMapper {

    public RoleDto toDto(Role entity) {
        if (entity == null) {
            return null;
        }

        return RoleDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .system(entity.isSystem())
            // Sorted by id for deterministic JSON output — Set iteration order is undefined.
            .permissions(entity.getPermissions().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(PermissionDto::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList())
            .build();
    }

    public PermissionDto toDto(Permission entity) {
        return PermissionDto.builder()
            .id(entity.getId())
            .entity(entity.getEntity())
            .field(entity.getField())
            .operation(entity.getOperation())
            .build();
    }

    public Role toEntity(RoleDto dto) {
        if (dto == null) {
            return null;
        }

        return Role.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .build();
    }

    public void updateEntityFromDto(RoleDto dto, Role entity) {
        if (dto == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
    }
}
