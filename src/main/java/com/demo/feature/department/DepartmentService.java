package com.demo.feature.department;

import com.demo.feature.security.rbac.access.AccessControl;
import com.demo.feature.security.rbac.access.FieldDiff;
import com.demo.feature.security.rbac.role.Operation;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing departments.
 *
 * <p>Access control treats a department as its own scope: a grant scoped to
 * department D lets its holder read/update/delete department D itself, while
 * creating departments (which have no id yet) needs a global grant.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class DepartmentService {

    private static final String NOT_FOUND = " not found";
    private static final String DEPARTMENT = DepartmentSecuredEntity.NAME;

    private final DepartmentRepository repository;
    private final DepartmentMapper mapper;
    private final AccessControl accessControl;

    public DepartmentDto createDepartment(DepartmentDto dto) {
        Set<Long> scope = accessControl.scopeOf(DEPARTMENT, dto);
        accessControl.requireAccess(DEPARTMENT, Operation.CREATE, scope);
        accessControl.requireFieldAccess(DEPARTMENT, Operation.CREATE, FieldDiff.suppliedFields(dto), scope);

        if (repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists");
        }

        Department saved = repository.save(mapper.toEntity(dto));
        log.info("Created department: {}", saved.getName());

        return accessControl.filterReadable(DEPARTMENT, mapper.toDto(saved));
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        DepartmentDto dto = mapper.toDto(load(id));
        accessControl.requireAccess(DEPARTMENT, Operation.READ, accessControl.scopeOf(DEPARTMENT, dto));
        return accessControl.filterReadable(DEPARTMENT, dto);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDto> getAllDepartments(Pageable pageable) {
        return repository.findAll(readScope(), pageable)
            .map(mapper::toDto)
            .map(dto -> accessControl.filterReadable(DEPARTMENT, dto));
    }

    /**
     * Filters departments by name and/or description (partial match);
     * null parameters are ignored. Restricted to departments the caller may read.
     */
    @Transactional(readOnly = true)
    public Page<DepartmentDto> filterDepartments(String name, String description, Pageable pageable) {
        return repository
                .findAll(DepartmentSpecifications.withFilters(name, description).and(readScope()), pageable)
                .map(mapper::toDto)
                .map(dto -> accessControl.filterReadable(DEPARTMENT, dto));
    }

    public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
        Department department = load(id);
        DepartmentDto stored = mapper.toDto(department);
        Set<Long> scope = new HashSet<>(accessControl.scopeOf(DEPARTMENT, stored));
        accessControl.requireAccess(DEPARTMENT, Operation.UPDATE, scope);
        accessControl.retainUnreadable(DEPARTMENT, stored, dto, scope);
        accessControl.requireFieldAccess(DEPARTMENT, Operation.UPDATE, FieldDiff.changedFields(stored, dto), scope);

        if (!department.getName().equals(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists");
        }

        mapper.updateEntityFromDto(dto, department);
        Department updated = repository.save(department);
        log.info("Updated department: {}", updated.getName());

        return accessControl.filterReadable(DEPARTMENT, mapper.toDto(updated));
    }

    /**
     * Deletes a department. Deletion is never blocked by existing assignments:
     * every join entity declares {@code ON DELETE CASCADE} on its
     * {@code department_id} foreign key, so the database removes the links for
     * every owner type (and any department-scoped role assignments). Nothing
     * needs registering here when a new owner type is added, and no owner type
     * can be forgotten.
     */
    public void deleteDepartment(Long id) {
        Department department = load(id);
        accessControl.requireAccess(DEPARTMENT, Operation.DELETE,
            accessControl.scopeOf(DEPARTMENT, mapper.toDto(department)));

        repository.delete(department);
        log.info("Deleted department with id {}; its assignments were cascaded by the database", id);
    }

    /**
     * Resolves one department by id for other features, owning the 404 behaviour.
     * Not access-checked: the calling feature enforces its own entity's permissions.
     */
    @Transactional(readOnly = true)
    public Department resolveDepartment(Long id) {
        return load(id);
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

    private Department load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department with id " + id + NOT_FOUND));
    }

    /**
     * Paged-read restriction: nothing for a global READ grant, otherwise only the
     * departments the caller holds a READ grant in.
     */
    private Specification<Department> readScope() {
        return accessControl.readableDepartments(DEPARTMENT)
            .map(DepartmentSpecifications::idIn)
            .orElseGet(Specification::unrestricted);
    }
}
