package com.demo.application.user;

import com.demo.application.security.auth.RoleManagementService;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.User;
import com.demo.domain.user.UserDto;
import com.demo.domain.user.UserMapper;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing users.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleManagementService roleManagementService;
    private final UserMapper userMapper;

    public UserDto createUser(UserDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        Role role = roleManagementService.resolveRole(dto.getRoleId());

        User user = userMapper.toEntity(dto);
        user.setRole(role);

        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager with id " + dto.getManagerId() + " not found"));
            user.setManager(manager);
        }

        User saved = userRepository.save(user);
        log.info("Created user: {}", saved.getUsername());

        return userMapper.toDto(saved);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
            .map(userMapper::toDto)
            .collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return userMapper.toDto(user);
    }

    public UserDto updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        // Check if username is being changed to an existing username
        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }

        // Check if email is being changed to an existing email
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        Role role = roleManagementService.resolveRole(dto.getRoleId());

        userMapper.updateEntityFromDto(dto, user);
        user.setRole(role);

        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager with id " + dto.getManagerId() + " not found"));
            user.setManager(manager);
        } else {
            user.setManager(null);
        }

        User updated = userRepository.save(user);
        log.info("Updated user: {}", updated.getUsername());

        return userMapper.toDto(updated);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }
}
