package com.demo.feature.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Department entity. Dynamic filtering is done through
 * {@link JpaSpecificationExecutor} with the specifications defined in
 * {@link DepartmentSpecifications} — prefer composing specifications over
 * adding new {@code @Query} filter methods.
 */
@Repository
public interface DepartmentRepository
        extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    Optional<Department> findByName(String name);

    boolean existsByName(String name);
}
