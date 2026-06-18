package com.demo.application.department;

import com.demo.domain.department.Department;
import com.demo.domain.department.DepartmentDto;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Management of departments. Membership of users/objects in departments is managed through
 * the user and computer-system endpoints (via their departmentIds).
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentDto create(DepartmentDto dto) {
        if (departmentRepository.findByName(dto.name()).isPresent()) {
            throw new DuplicateResourceException("Department '" + dto.name() + "' already exists");
        }
        Department saved = departmentRepository.save(Department.builder().name(dto.name()).build());
        log.info("Created department: {}", saved.getName());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAll() {
        return departmentRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDto getById(Long id) {
        return toDto(find(id));
    }

    public DepartmentDto update(Long id, DepartmentDto dto) {
        Department department = find(id);
        if (!department.getName().equals(dto.name()) && departmentRepository.findByName(dto.name()).isPresent()) {
            throw new DuplicateResourceException("Department '" + dto.name() + "' already exists");
        }
        department.setName(dto.name());
        return toDto(departmentRepository.save(department));
    }

    public void delete(Long id) {
        departmentRepository.delete(find(id));
    }

    private Department find(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department with id " + id + " not found"));
    }

    private DepartmentDto toDto(Department department) {
        return new DepartmentDto(department.getId(), department.getName());
    }
}
