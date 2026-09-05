package com.demo.feature.computersystem;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentMapper;
import com.demo.feature.department.DepartmentRepository;
import com.demo.feature.user.User;
import com.demo.feature.user.UserRepository;
import com.demo.platform.config.JpaConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the fetching strategy for department links: {@code @BatchSize} on the
 * collection, never a collection-fetching {@code @EntityGraph} on a paged query.
 *
 * <p>Swapping the batch size for an entity graph on
 * {@code findAll(Specification, Pageable)} makes Hibernate join the collection
 * and paginate in memory (HHH000104) — it still returns the right answer, so
 * only a query-count assertion catches it.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(JpaConfig.class)
class ComputerSystemDepartmentFetchIT {

    private static final int DEPARTMENT_COUNT = 3;
    private static final int SYSTEM_COUNT = 12;

    @Autowired
    private ComputerSystemRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final ComputerSystemMapper mapper = new ComputerSystemMapper(new DepartmentMapper());

    private List<Department> departments;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
            .username("owner")
            .email("owner@example.com")
            .build());

        departments = new ArrayList<>();
        for (int i = 0; i < DEPARTMENT_COUNT; i++) {
            departments.add(departmentRepository.save(Department.builder().name("DEPT-" + i).build()));
        }

        for (int i = 0; i < SYSTEM_COUNT; i++) {
            ComputerSystem system = ComputerSystem.builder()
                .hostname("SERVER-" + i)
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .macAddress(String.format("00:1A:2B:3C:4D:%02X", i))
                .ipAddress("192.168.1." + (100 + i))
                .networkName("PROD-NETWORK")
                .systemUser(owner)
                .build();
            // Every system belongs to every department, so a small page and a
            // large one touch the same set of departments — the only thing that
            // varies between the two measurements below is the number of rows.
            departments.forEach(department -> system.getDepartmentLinks().add(
                ComputerSystemDepartment.builder()
                    .computerSystem(system)
                    .department(department)
                    .build()));
            repository.save(system);
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void pagedReadsIssueAConstantNumberOfQueries() {
        long smallPage = countQueriesMappingPage(3);
        long largePage = countQueriesMappingPage(SYSTEM_COUNT);

        assertEquals(smallPage, largePage,
            "query count must not grow with page size — check @BatchSize on "
                + "ComputerSystem.departmentLinks and that no collection entity graph "
                + "was added to a paged query method");
    }

    @Test
    void duplicateAssignmentIsRejected() {
        ComputerSystem system = repository.findByHostname("SERVER-0").orElseThrow();
        system.getDepartmentLinks().add(ComputerSystemDepartment.builder()
            .computerSystem(system)
            .department(departments.get(0))
            .build());

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(system);
            entityManager.flush();
        });
    }

    /**
     * Maps a page through the real mapper, so the department links are actually
     * initialised rather than left as untouched proxies.
     */
    private long countQueriesMappingPage(int pageSize) {
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        repository.findAll(PageRequest.of(0, pageSize))
            .map(mapper::toDto)
            .forEach(dto -> assertEquals(DEPARTMENT_COUNT, dto.getDepartments().size()));

        return statistics.getPrepareStatementCount();
    }
}
