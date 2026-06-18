package com.demo.application.computersystem;

import com.demo.application.department.DepartmentRepository;
import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.domain.computersystem.ComputerSystemMapper;
import com.demo.domain.department.Department;
import com.demo.domain.user.User;
import com.demo.application.user.UserRepository;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import com.demo.shared.security.CurrentUserService;
import com.demo.shared.security.FieldProjectionService;
import com.demo.shared.security.GrantService;
import com.demo.shared.security.ScopeResult;
import com.demo.shared.security.ScopeSpecifications;
import com.demo.domain.computersystem.ComputerSystem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service layer for managing computer systems with database circuit breaker protection
 * and department/role-based access control. Object-level scope is enforced per request
 * against the {@link CurrentUserService current user}; list queries are scoped in-SQL via
 * {@link ScopeSpecifications} so pagination stays correct; field visibility/writability is
 * applied by {@link FieldProjectionService}.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ComputerSystemService {

    private static final String NOT_FOUND = " not found";
    private static final String ID_PREFIX = "Computer system with id ";
    private static final String RESOURCE_TYPE = "ComputerSystem";

    private final ComputerSystemRepository repository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ComputerSystemMapper mapper;
    private final CurrentUserService currentUserService;
    private final GrantService grantService;
    private final FieldProjectionService fieldProjectionService;

    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "createComputerSystemFallback")
    public ComputerSystemDto createComputerSystem(ComputerSystemDto dto) {
        requireUnique(dto, null);

        ComputerSystem computerSystem = mapper.toEntity(dto);
        computerSystem.setSystemUser(resolveUser(dto.getUserId()));
        computerSystem.setDepartments(resolveDepartments(dto.getDepartmentIds()));

        User currentUser = currentUserService.requireCurrentUser();
        if (!grantService.canAccess(currentUser, computerSystem, RESOURCE_TYPE, GrantService.WRITE)) {
            throw new AccessDeniedException("Not permitted to create a computer system in these departments");
        }

        ComputerSystem saved = repository.save(computerSystem);
        return fieldProjectionService.filterReadable(currentUser, saved, mapper.toDto(saved), RESOURCE_TYPE);
    }

    public ComputerSystemDto createComputerSystemFallback(ComputerSystemDto dto, CallNotPermittedException ex) {
        log.error("Database circuit breaker OPEN: Cannot create computer system - database unavailable");
        throw new IllegalStateException("Database service temporarily unavailable. Please try again later.");
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "getComputerSystemByIdFallback")
    public ComputerSystemDto getComputerSystemById(Long id) {
        ComputerSystem computerSystem = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ID_PREFIX + id + NOT_FOUND));
        User currentUser = requireAccess(computerSystem, GrantService.READ);
        return fieldProjectionService.filterReadable(currentUser, computerSystem, mapper.toDto(computerSystem), RESOURCE_TYPE);
    }

    public ComputerSystemDto getComputerSystemByIdFallback(Long id, CallNotPermittedException ex) {
        log.error("Database circuit breaker OPEN: Cannot retrieve computer system {} - database unavailable", id);
        throw new IllegalStateException("Database service temporarily unavailable. Please try again later.");
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "getAllComputerSystemsFallback")
    public Page<ComputerSystemDto> getAllComputerSystems(Pageable pageable) {
        User currentUser = currentUserService.requireCurrentUser();
        return repository.findAll(readScopeSpecification(currentUser), pageable)
                .map(cs -> fieldProjectionService.filterReadable(currentUser, cs, mapper.toDto(cs), RESOURCE_TYPE));
    }

    public Page<ComputerSystemDto> getAllComputerSystemsFallback(Pageable pageable, CallNotPermittedException ex) {
        log.error("Database circuit breaker OPEN: Cannot retrieve computer systems - database unavailable");
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "filterComputerSystemsFallback")
    public Page<ComputerSystemDto> filterComputerSystems(String hostname, Long departmentId, Long userId, Pageable pageable) {
        User currentUser = currentUserService.requireCurrentUser();
        Specification<ComputerSystem> spec = readScopeSpecification(currentUser)
                .and(ComputerSystemSpecifications.filter(hostname, departmentId, userId));
        return repository.findAll(spec, pageable)
                .map(cs -> fieldProjectionService.filterReadable(currentUser, cs, mapper.toDto(cs), RESOURCE_TYPE));
    }

    public Page<ComputerSystemDto> filterComputerSystemsFallback(String hostname, Long departmentId, Long userId,
                                                                 Pageable pageable, CallNotPermittedException ex) {
        log.error("Database circuit breaker OPEN: Cannot filter computer systems - database unavailable");
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "updateComputerSystemFallback")
    public ComputerSystemDto updateComputerSystem(Long id, ComputerSystemDto dto) {
        ComputerSystem computerSystem = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ID_PREFIX + id + NOT_FOUND));
        User currentUser = requireAccess(computerSystem, GrantService.WRITE);
        requireUnique(dto, computerSystem);

        // Validate field-level write policy against the current persisted state before mutating.
        fieldProjectionService.validateWritable(currentUser, computerSystem, mapper.toDto(computerSystem), dto, RESOURCE_TYPE);

        mapper.updateEntityFromDto(dto, computerSystem);
        computerSystem.setSystemUser(resolveUser(dto.getUserId()));
        computerSystem.setDepartments(resolveDepartments(dto.getDepartmentIds()));

        ComputerSystem updated = repository.save(computerSystem);
        return fieldProjectionService.filterReadable(currentUser, updated, mapper.toDto(updated), RESOURCE_TYPE);
    }

    public ComputerSystemDto updateComputerSystemFallback(Long id, ComputerSystemDto dto, CallNotPermittedException ex) {
        log.error("Database circuit breaker OPEN: Cannot update computer system {} - database unavailable", id);
        throw new IllegalStateException("Database service temporarily unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "deleteComputerSystemFallback")
    public void deleteComputerSystem(Long id) {
        ComputerSystem computerSystem = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ID_PREFIX + id + NOT_FOUND));
        requireAccess(computerSystem, GrantService.DELETE);
        repository.deleteById(id);
    }

    public void deleteComputerSystemFallback(Long id, CallNotPermittedException ex) {
        log.error("Database circuit breaker OPEN: Cannot delete computer system {} - database unavailable", id);
        throw new IllegalStateException("Database service temporarily unavailable. Please try again later.");
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "getComputerSystemByHostnameFallback")
    public ComputerSystemDto getComputerSystemByHostname(String hostname) {
        ComputerSystem computerSystem = repository.findByHostname(hostname)
                .orElseThrow(() -> new ResourceNotFoundException("Computer system with hostname " + hostname + NOT_FOUND));
        User currentUser = requireAccess(computerSystem, GrantService.READ);
        return fieldProjectionService.filterReadable(currentUser, computerSystem, mapper.toDto(computerSystem), RESOURCE_TYPE);
    }

    public ComputerSystemDto getComputerSystemByHostnameFallback(String hostname, CallNotPermittedException ex) {
        log.error("Database circuit breaker OPEN: Cannot retrieve computer system {} - database unavailable", hostname);
        throw new IllegalStateException("Database service temporarily unavailable. Please try again later.");
    }

    private Specification<ComputerSystem> readScopeSpecification(User currentUser) {
        ScopeResult scope = grantService.resolveScope(currentUser, RESOURCE_TYPE, GrantService.READ);
        return ScopeSpecifications.forComputerSystem(scope, currentUser.getId());
    }

    private User requireAccess(ComputerSystem computerSystem, String operation) {
        User currentUser = currentUserService.requireCurrentUser();
        if (!grantService.canAccess(currentUser, computerSystem, RESOURCE_TYPE, operation)) {
            throw new AccessDeniedException("Not permitted to " + operation + " this computer system");
        }
        return currentUser;
    }

    private void requireUnique(ComputerSystemDto dto, ComputerSystem existing) {
        if ((existing == null || !existing.getHostname().equals(dto.getHostname()))
                && repository.findByHostname(dto.getHostname()).isPresent()) {
            throw new DuplicateResourceException("Computer system with hostname " + dto.getHostname() + " already exists");
        }
        if ((existing == null || !existing.getMacAddress().equals(dto.getMacAddress()))
                && repository.findByMacAddress(dto.getMacAddress()).isPresent()) {
            throw new DuplicateResourceException("Computer system with MAC address " + dto.getMacAddress() + " already exists");
        }
        if ((existing == null || !existing.getIpAddress().equals(dto.getIpAddress()))
                && repository.findByIpAddress(dto.getIpAddress()).isPresent()) {
            throw new DuplicateResourceException("Computer system with IP address " + dto.getIpAddress() + " already exists");
        }
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + NOT_FOUND));
    }

    private Set<Department> resolveDepartments(Set<Long> departmentIds) {
        List<Department> found = departmentRepository.findAllById(departmentIds);
        if (found.size() != departmentIds.size()) {
            throw new ResourceNotFoundException("One or more departments not found: " + departmentIds);
        }
        return new LinkedHashSet<>(found);
    }
}
