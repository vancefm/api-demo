package com.demo.feature.user;

import com.demo.feature.department.DepartmentService;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
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
    private final UserMapper userMapper;
    private final DepartmentService departmentService;

    public UserDto createUser(UserDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User with username '" + dto.getUsername() + "' already exists");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        User user = userMapper.toEntity(dto);
        user.setDepartments(departmentService.resolveDepartments(dto.getDepartmentIds()));

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

        userMapper.updateEntityFromDto(dto, user);
        user.setDepartments(departmentService.resolveDepartments(dto.getDepartmentIds()));

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
