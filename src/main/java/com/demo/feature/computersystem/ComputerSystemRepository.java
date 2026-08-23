package com.demo.feature.computersystem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Dynamic filtering is done through {@link JpaSpecificationExecutor} with the
 * specifications defined in {@link ComputerSystemSpecifications} — prefer
 * composing specifications over adding new {@code @Query} filter methods.
 */
@Repository
public interface ComputerSystemRepository
        extends JpaRepository<ComputerSystem, Long>, JpaSpecificationExecutor<ComputerSystem> {

    Optional<ComputerSystem> findByHostname(String hostname);

    Optional<ComputerSystem> findByMacAddress(String macAddress);

    Optional<ComputerSystem> findByIpAddress(String ipAddress);

    /**
     * Removes all computer-system associations for a department
     * (cascade-dissociate on department deletion). Deliberately a native
     * {@code @Modifying} query rather than a Specification or entity-level
     * removal: the join table has no entity mapping (so JPQL/Criteria cannot
     * target it), and loading every affected system just to mutate its
     * collection would be a bulk N+1. Callers must not have affected entities
     * loaded in the persistence context (the bulk delete bypasses it).
     */
    @Modifying
    @Query(value = "DELETE FROM computer_system_departments WHERE department_id = :departmentId", nativeQuery = true)
    void removeDepartmentAssociations(@Param("departmentId") Long departmentId);

    // In future methods be conscious potential of n+1 problem when using JPA
    // and fetching related entities. A work around for this would be to use
    // fetch joins or entity graphs.
}
