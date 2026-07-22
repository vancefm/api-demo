package com.demo.feature.user;

import org.springframework.stereotype.Component;

/**
 * Maps between {@link User} entities and {@link UserDto}.
 *
 * Note: The manager relationship requires a database lookup, so toEntity and
 * updateEntityFromDto do not set the manager field. The service layer is
 * responsible for resolving and setting the manager entity.
 */
@Component
public class UserMapper {

    public UserDto toDto(User entity) {
        if (entity == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setDepartment(entity.getDepartment());
        dto.setManagerId(entity.getManager() != null ? entity.getManager().getId() : null);
        return dto;
    }

    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        return User.builder()
            .id(dto.getId())
            .username(dto.getUsername())
            .email(dto.getEmail())
            .department(dto.getDepartment())
            .build();
    }

    public void updateEntityFromDto(UserDto dto, User entity) {
        if (dto == null) {
            return;
        }

        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setDepartment(dto.getDepartment());
    }
}
