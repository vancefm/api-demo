package com.demo.feature.security.rbac.role;

import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Maps between {@link Role}/{@link Permission} and their DTOs.
 * Permissions are not mapped inbound here: {@code RoleService} validates each
 * one against the secured-entity registry before adding it.
 */
@Component
public class RoleMapper {

    /**
     * Permissions are values with no id, so JSON order is made deterministic by
     * sorting on the grant itself — Set iteration order is undefined.
     */
    private static final Comparator<PermissionDto> BY_GRANT =
        Comparator.comparing(PermissionDto::getEntity)
            .thenComparing(PermissionDto::getField)
            .thenComparing(permission -> permission.getOperation().name());

    public RoleDto toDto(Role entity) {
        if (entity == null) {
            return null;
        }

        return RoleDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .system(entity.isSystem())
            .permissions(entity.getPermissions().stream()
                .map(this::toDto)
                .sorted(BY_GRANT)
                .toList())
            .build();
    }

    public PermissionDto toDto(Permission permission) {
        return PermissionDto.builder()
            .entity(permission.getEntity())
            .field(permission.getField())
            .operation(permission.getOperation())
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
