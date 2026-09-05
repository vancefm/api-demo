package com.demo.feature.computersystem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Dynamic filtering is done through {@link JpaSpecificationExecutor} with the
 * specifications defined in {@link ComputerSystemSpecifications} — prefer
 * composing specifications over adding new {@code @Query} filter methods.
 *
 * <p>Association fetching follows one rule: {@code @EntityGraph} on
 * single-entity reads, {@code @BatchSize} (declared on the collection itself)
 * for paged reads. A collection-fetching entity graph combined with pagination
 * makes Hibernate join the collection and paginate in memory (HHH000104),
 * loading the entire result set — so paged methods are deliberately left
 * ungraphed for {@code departmentLinks}.
 *
 * <p>Deleting a department does not need anything here: the join entity's
 * foreign keys are declared {@code ON DELETE CASCADE}, so the database removes
 * the links.
 */
@Repository
public interface ComputerSystemRepository
        extends JpaRepository<ComputerSystem, Long>, JpaSpecificationExecutor<ComputerSystem> {

    @Override
    @EntityGraph(attributePaths = {"systemUser", "departmentLinks", "departmentLinks.department"})
    Optional<ComputerSystem> findById(Long id);

    @EntityGraph(attributePaths = {"systemUser", "departmentLinks", "departmentLinks.department"})
    Optional<ComputerSystem> findByHostname(String hostname);

    Optional<ComputerSystem> findByMacAddress(String macAddress);

    Optional<ComputerSystem> findByIpAddress(String ipAddress);

    /**
     * Paged reads graph only {@code systemUser} — a to-one association, so it
     * joins without triggering in-memory pagination. {@code departmentLinks} is
     * left to {@code @BatchSize} for the reason given in the class javadoc.
     */
    @Override
    @EntityGraph(attributePaths = {"systemUser"})
    Page<ComputerSystem> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"systemUser"})
    Page<ComputerSystem> findAll(Specification<ComputerSystem> spec, Pageable pageable);
}
