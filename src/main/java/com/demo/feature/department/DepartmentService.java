package com.demo.feature.department;

import com.demo.feature.computersystem.ComputerSystemRepository;
import com.demo.feature.user.UserRepository;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing departments.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class DepartmentService {

    private static final String NOT_FOUND = " not found";

    private final DepartmentRepository repository;
    private final UserRepository userRepository;
    private final ComputerSystemRepository computerSystemRepository;
    private final DepartmentMapper mapper;

    public DepartmentDto createDepartment(DepartmentDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists");
        }

        Department saved = repository.save(mapper.toEntity(dto));
        log.info("Created department: {}", saved.getName());

        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        Department department = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department with id " + id + NOT_FOUND));
        return mapper.toDto(department);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDto> getAllDepartments(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
        Department department = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department with id " + id + NOT_FOUND));

        if (!department.getName().equals(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists");
        }

        mapper.updateEntityFromDto(dto, department);
        Department updated = repository.save(department);
        log.info("Updated department: {}", updated.getName());

        return mapper.toDto(updated);
    }

    /**
     * Deletes a department, silently dissociating it from every user and
     * computer system that references it (cascade-dissociate — deletion is
     * never blocked by existing assignments).
     */
    public void deleteDepartment(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Department with id " + id + NOT_FOUND);
        }

        userRepository.removeDepartmentAssociations(id);
        computerSystemRepository.removeDepartmentAssociations(id);
        repository.deleteById(id);
        log.info("Deleted department with id {} and removed its associations", id);
    }

    /**
     * Resolves department IDs to entities for the User/ComputerSystem services.
     * Null or empty input yields an empty set (departments are optional);
     * any ID that doesn't exist fails the whole call with a 404.
     */
    @Transactional(readOnly = true)
    public Set<Department> resolveDepartments(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Department> found = repository.findAllById(departmentIds);
        if (found.size() != new HashSet<>(departmentIds).size()) {
            Set<Long> foundIds = found.stream().map(Department::getId).collect(Collectors.toSet());
            List<Long> missing = departmentIds.stream().distinct()
                .filter(id -> !foundIds.contains(id))
                .toList();
            throw new ResourceNotFoundException("Department(s) with id " + missing + NOT_FOUND);
        }

        return new HashSet<>(found);
    }
}
