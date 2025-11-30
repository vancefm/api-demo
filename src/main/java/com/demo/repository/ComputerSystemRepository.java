package com.demo.repository;

import com.demo.model.ComputerSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComputerSystemRepository extends JpaRepository<ComputerSystem, Long> {

    Optional<ComputerSystem> findByHostname(String hostname);

    Optional<ComputerSystem> findByMacAddress(String macAddress);

    Optional<ComputerSystem> findByIpAddress(String ipAddress);

    @Query("SELECT cs FROM ComputerSystem cs WHERE " +
           "(:hostname IS NULL OR cs.hostname LIKE %:hostname%) AND " +
           "(:department IS NULL OR cs.department = :department) AND " +
           "(:user IS NULL OR cs.systemUser LIKE %:user%)")
    Page<ComputerSystem> findByFilters(
            @Param("hostname") String hostname,
            @Param("department") String department,
            @Param("user") String user,
            Pageable pageable
    );

    // In future methods be conscious potential of n+1 problem when using JPA
    // and fetching related entities. A work around for this would be to use
    // fetch joins or entity graphs.
}
