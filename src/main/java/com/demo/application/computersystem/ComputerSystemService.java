package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import com.demo.domain.computersystem.ComputerSystem;
import com.demo.application.computersystem.ComputerSystemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Service layer for managing computer systems with circuit breaker protection.
 *
 * Database circuit breaker protects against:
 * - Connection pool exhaustion
 * - Slow database queries
 * - Database service unavailability
 *
 * When database becomes unreliable, circuit breaker:
 * - Returns empty results gracefully
 * - Prevents cascading failures
 * - Allows database time to recover
 */
@Service
@Transactional
public class ComputerSystemService {

    private static final Logger logger = LoggerFactory.getLogger(ComputerSystemService.class);
    private static final String NOT_FOUND = " not found";
    private final ComputerSystemRepository repository;
    private final ComputerSystemMapper mapper;

    public ComputerSystemService(ComputerSystemRepository repository, ComputerSystemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Creates new computer system with database circuit breaker protection.
     *
     * Circuit breaker opens if 60% of the last 20 calls fail or are slow (>3s).
     * If circuit opens, returns empty result gracefully instead of timing out.
     *
     * @param dto Computer system data transfer object
     * @return Saved computer system DTO
     * @throws DuplicateResourceException If hostname, MAC, or IP already exists
     */
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "createComputerSystemFallback")
    public ComputerSystemDto createComputerSystem(ComputerSystemDto dto) {
        if (repository.findByHostname(dto.getHostname()).isPresent()) {
            throw new DuplicateResourceException("Computer system with hostname " + dto.getHostname() + " already exists");
        }

        if (repository.findByMacAddress(dto.getMacAddress()).isPresent()) {
            throw new DuplicateResourceException("Computer system with MAC address " + dto.getMacAddress() + " already exists");
        }

        if (repository.findByIpAddress(dto.getIpAddress()).isPresent()) {
            throw new DuplicateResourceException("Computer system with IP address " + dto.getIpAddress() + " already exists");
        }

        ComputerSystem computerSystem = mapper.toEntity(dto);
        ComputerSystem savedSystem = repository.save(computerSystem);

        return mapper.toDto(savedSystem);
    }

    /**
     * Fallback for createComputerSystem when database circuit breaker is OPEN.
     * Returns an error response instead of timeout.
     */
    public ComputerSystemDto createComputerSystemFallback(ComputerSystemDto dto,
                                                         CallNotPermittedException ex) {
        logger.error("Database circuit breaker OPEN: Cannot create computer system - database unavailable");
        throw new RuntimeException("Database service temporarily unavailable. Please try again later.");
    }

    /**
     * Retrieves computer system by ID with database circuit breaker protection.
     *
     * @param id Computer system ID
     * @return Computer system DTO
     * @throws ResourceNotFoundException If not found
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "getComputerSystemByIdFallback")
    public ComputerSystemDto getComputerSystemById(Long id) {
        ComputerSystem computerSystem = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Computer system with id " + id + NOT_FOUND));

        return mapper.toDto(computerSystem);
    }

    /**
     * Fallback for getComputerSystemById when database circuit breaker is OPEN.
     */
    public ComputerSystemDto getComputerSystemByIdFallback(Long id,
                                                          CallNotPermittedException ex) {
        logger.error("Database circuit breaker OPEN: Cannot retrieve computer system {} - database unavailable", id);
        throw new RuntimeException("Database service temporarily unavailable. Please try again later.");
    }

    /**
     * Retrieves all computer systems with pagination and circuit breaker protection.
     *
     * @param pageable Pagination parameters
     * @return Page of computer systems
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "getAllComputerSystemsFallback")
    public Page<ComputerSystemDto> getAllComputerSystems(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    /**
     * Fallback for getAllComputerSystems when database circuit breaker is OPEN.
     * Returns empty page to indicate service unavailable.
     */
    public Page<ComputerSystemDto> getAllComputerSystemsFallback(Pageable pageable,
                                                                CallNotPermittedException ex) {
        logger.error("Database circuit breaker OPEN: Cannot retrieve computer systems - database unavailable");
        // Return empty page instead of error
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    /**
     * Filters computer systems by hostname, department, or user with circuit breaker protection.
     *
     * @param hostname Department to filter by
     * @param department Department to filter by
     * @param user User to filter by
     * @param pageable Pagination parameters
     * @return Filtered page of computer systems
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "filterComputerSystemsFallback")
    public Page<ComputerSystemDto> filterComputerSystems(
            String hostname,
            String department,
            String user,
            Pageable pageable) {
        return repository.findByFilters(hostname, department, user, pageable).map(mapper::toDto);
    }

    /**
     * Fallback for filterComputerSystems when database circuit breaker is OPEN.
     * Returns empty page when database is unavailable.
     */
    public Page<ComputerSystemDto> filterComputerSystemsFallback(
            String hostname,
            String department,
            String user,
            Pageable pageable,
            CallNotPermittedException ex) {
        logger.error("Database circuit breaker OPEN: Cannot filter computer systems - database unavailable");
        // Return empty page indicating service unavailable
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    /**
     * Updates computer system with circuit breaker protection.
     *
     * @param id Computer system ID to update
     * @param dto Updated computer system data
     * @return Updated computer system DTO
     * @throws ResourceNotFoundException If system not found
     */
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "updateComputerSystemFallback")
    public ComputerSystemDto updateComputerSystem(Long id, ComputerSystemDto dto) {
        ComputerSystem computerSystem = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Computer system with id " + id + NOT_FOUND));

        if (!computerSystem.getHostname().equals(dto.getHostname()) &&
            repository.findByHostname(dto.getHostname()).isPresent()) {
            throw new DuplicateResourceException("Computer system with hostname " + dto.getHostname() + " already exists");
        }

        if (!computerSystem.getMacAddress().equals(dto.getMacAddress()) &&
            repository.findByMacAddress(dto.getMacAddress()).isPresent()) {
            throw new DuplicateResourceException("Computer system with MAC address " + dto.getMacAddress() + " already exists");
        }

        if (!computerSystem.getIpAddress().equals(dto.getIpAddress()) &&
            repository.findByIpAddress(dto.getIpAddress()).isPresent()) {
            throw new DuplicateResourceException("Computer system with IP address " + dto.getIpAddress() + " already exists");
        }

        mapper.updateEntityFromDto(dto, computerSystem);

        ComputerSystem updatedSystem = repository.save(computerSystem);

        return mapper.toDto(updatedSystem);
    }

    /**
     * Fallback for updateComputerSystem when database circuit breaker is OPEN.
     */
    public ComputerSystemDto updateComputerSystemFallback(Long id, ComputerSystemDto dto,
                                                         CallNotPermittedException ex) {
        logger.error("Database circuit breaker OPEN: Cannot update computer system {} - database unavailable", id);
        throw new RuntimeException("Database service temporarily unavailable. Please try again later.");
    }

    /**
     * Deletes computer system with circuit breaker protection.
     *
     * @param id Computer system ID to delete
     * @throws ResourceNotFoundException If system not found
     */
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "deleteComputerSystemFallback")
    public void deleteComputerSystem(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Computer system with id " + id + NOT_FOUND);
        }

        repository.deleteById(id);
    }

    /**
     * Fallback for deleteComputerSystem when database circuit breaker is OPEN.
     */
    public void deleteComputerSystemFallback(Long id,
                                            CallNotPermittedException ex) {
        logger.error("Database circuit breaker OPEN: Cannot delete computer system {} - database unavailable", id);
        throw new RuntimeException("Database service temporarily unavailable. Please try again later.");
    }

    /**
     * Retrieves computer system by hostname with circuit breaker protection.
     *
     * @param hostname Computer system hostname
     * @return Computer system DTO
     * @throws ResourceNotFoundException If not found
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "getComputerSystemByHostnameFallback")
    public ComputerSystemDto getComputerSystemByHostname(String hostname) {
        ComputerSystem computerSystem = repository.findByHostname(hostname)
                .orElseThrow(() -> new ResourceNotFoundException("Computer system with hostname " + hostname + NOT_FOUND));

        return mapper.toDto(computerSystem);
    }

    /**
     * Fallback for getComputerSystemByHostname when database circuit breaker is OPEN.
     */
    public ComputerSystemDto getComputerSystemByHostnameFallback(String hostname,
                                                                CallNotPermittedException ex) {
        logger.error("Database circuit breaker OPEN: Cannot retrieve computer system {} - database unavailable", hostname);
        throw new RuntimeException("Database service temporarily unavailable. Please try again later.");
    }
}

