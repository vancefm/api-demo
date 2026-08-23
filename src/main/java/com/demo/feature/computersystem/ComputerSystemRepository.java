package com.demo.feature.computersystem;

import com.demo.feature.computersystem.ComputerSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComputerSystemRepository extends JpaRepository<ComputerSystem, Long> {

    Optional<ComputerSystem> findByHostname(String hostname);

    Optional<ComputerSystem> findByMacAddress(String macAddress);

    Optional<ComputerSystem> findByIpAddress(String ipAddress);

    // The department filter uses a correlated EXISTS subquery rather than a join:
    // a join would fan out one row per associated department, inflating Page
    // totals, and would wrongly exclude zero-department systems when unfiltered.
    @Query("SELECT cs FROM ComputerSystem cs WHERE " +
           "(:hostname IS NULL OR cs.hostname LIKE %:hostname%) AND " +
           "(:departmentId IS NULL OR EXISTS (SELECT 1 FROM cs.departments d WHERE d.id = :departmentId)) AND " +
           "(:userId IS NULL OR cs.systemUser.id = :userId)")
    Page<ComputerSystem> findByFilters(
            @Param("hostname") String hostname,
            @Param("departmentId") Long departmentId,
            @Param("userId") Long userId,
            Pageable pageable
    );

    /**
     * Removes all computer-system associations for a department
     * (cascade-dissociate on department deletion). Native SQL — the join table
     * has no entity mapping.
     */
    @Modifying
    @Query(value = "DELETE FROM computer_system_departments WHERE department_id = :departmentId", nativeQuery = true)
    void removeDepartmentAssociations(@Param("departmentId") Long departmentId);

    // In future methods be conscious potential of n+1 problem when using JPA
    // and fetching related entities. A work around for this would be to use
    // fetch joins or entity graphs.
}
