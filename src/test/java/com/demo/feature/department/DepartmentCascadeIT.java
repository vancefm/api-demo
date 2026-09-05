package com.demo.feature.department;

import com.demo.feature.computersystem.ComputerSystem;
import com.demo.feature.computersystem.ComputerSystemDepartment;
import com.demo.feature.computersystem.ComputerSystemRepository;
import com.demo.feature.user.User;
import com.demo.feature.user.UserDepartment;
import com.demo.feature.user.UserRepository;
import com.demo.platform.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that department dissociation really is enforced by the database.
 *
 * <p>This is the safety net for the {@code @OnDelete(CASCADE)} annotations on
 * the join entities. They are invisible at compile time and silently stop
 * working if the foreign keys are ever created some other way — a Flyway
 * migration, a hand-written schema — at which point deleting a department would
 * fail with a foreign-key violation instead. Nothing else in the test suite
 * would catch that, because no application code performs the dissociation any
 * more.
 */
@DataJpaTest
@Import(JpaConfig.class)
class DepartmentCascadeIT {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComputerSystemRepository computerSystemRepository;

    @Autowired
    private EntityManager entityManager;

    private Department department;
    private User user;
    private ComputerSystem computerSystem;

    @BeforeEach
    void setUp() {
        department = departmentRepository.save(Department.builder()
            .name("IT")
            .description("Information Technology")
            .build());

        user = User.builder()
            .username("john.doe")
            .email("john.doe@example.com")
            .build();
        user.getDepartmentLinks().add(UserDepartment.builder()
            .user(user)
            .department(department)
            .build());
        user = userRepository.save(user);

        computerSystem = ComputerSystem.builder()
            .hostname("SERVER-001")
            .manufacturer("Dell")
            .model("PowerEdge R750")
            .macAddress("00:1A:2B:3C:4D:5E")
            .ipAddress("192.168.1.100")
            .networkName("PROD-NETWORK")
            .systemUser(user)
            .build();
        computerSystem.getDepartmentLinks().add(ComputerSystemDepartment.builder()
            .computerSystem(computerSystem)
            .department(department)
            .build());
        computerSystem = computerSystemRepository.save(computerSystem);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deletingDepartmentCascadesLinksForEveryOwnerType() {
        departmentRepository.deleteById(department.getId());
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, countLinks("user_departments"),
            "user_departments rows should be cascaded by the department FK");
        assertEquals(0, countLinks("computer_system_departments"),
            "computer_system_departments rows should be cascaded by the department FK");

        // The owners themselves must survive — only the association is removed.
        assertTrue(userRepository.findById(user.getId()).isPresent());
        assertTrue(computerSystemRepository.findById(computerSystem.getId()).isPresent());
    }

    @Test
    void deletingOwnerCascadesItsOwnLinksAndLeavesTheDepartment() {
        computerSystemRepository.deleteById(computerSystem.getId());
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, countLinks("computer_system_departments"));
        // Other owners' links and the department itself are untouched.
        assertEquals(1, countLinks("user_departments"));
        assertTrue(departmentRepository.findById(department.getId()).isPresent());
    }

    @Test
    void departmentForeignKeysAreDeclaredOnDeleteCascade() {
        assertEquals("CASCADE", deleteRuleFor("computer_system_departments"));
        assertEquals("CASCADE", deleteRuleFor("user_departments"));
    }

    private long countLinks(String table) {
        return ((Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM " + table)
            .getSingleResult()).longValue();
    }

    /**
     * Reads the cascade straight out of the generated schema, so a failure
     * points at the DDL rather than at application behaviour.
     */
    private String deleteRuleFor(String table) {
        return (String) entityManager.createNativeQuery("""
                SELECT rc.DELETE_RULE
                FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                  ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
                 AND kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
                WHERE UPPER(kcu.TABLE_NAME) = UPPER(:table)
                  AND UPPER(kcu.COLUMN_NAME) = 'DEPARTMENT_ID'
                """)
            .setParameter("table", table)
            .getSingleResult();
    }
}
