package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComputerSystemRepository
        extends JpaRepository<ComputerSystem, Long>, JpaSpecificationExecutor<ComputerSystem> {

    Optional<ComputerSystem> findByHostname(String hostname);

    Optional<ComputerSystem> findByMacAddress(String macAddress);

    Optional<ComputerSystem> findByIpAddress(String ipAddress);

    // Filtering + authorization scoping are expressed as JPA Specifications
    // (see ComputerSystemSpecifications / ScopeSpecifications) so they compose
    // and are applied in-query, keeping pagination counts correct.
}
