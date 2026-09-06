package com.demo.feature.security.rbac.assignment;
import com.demo.feature.security.rbac.role.Role;
import com.demo.feature.security.rbac.role.RoleRepository;
import com.demo.feature.security.rbac.role.Operation;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentRepository;
import com.demo.feature.user.User;
import com.demo.feature.user.UserRepository;
import com.demo.platform.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Role assignments against a real schema: the graphed lookup used by access
 * control, the unique grant constraint, and the database cascades from user and
 * role (the department cascade is covered with the other owner types in
 * {@code DepartmentCascadeIT}).
 */
@DataJpaTest
@Import(JpaConfig.class)
class RoleAssignmentRepositoryIT {

    @Autowired
    private RoleAssignmentRepository repository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Role role;
    private Department it;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder().username("alice").email("alice@example.com").build());
        role = Role.builder().name("Department User").build();
        role.addPermission("User", "firstName", Operation.READ);
        role = roleRepository.save(role);
        it = departmentRepository.save(Department.builder().name("IT").build());
        entityManager.flush();
    }

    private RoleAssignment grant(Department department) {
        return repository.save(RoleAssignment.builder().user(user).role(role).department(department).build());
    }

    @Test
    void findByUserIdLoadsRolePermissionsAndDepartment() {
        grant(it);
        grant(null);
        entityManager.flush();
        entityManager.clear();

        List<RoleAssignment> assignments = repository.findByUserId(user.getId());

        assertEquals(2, assignments.size());
        assertTrue(assignments.stream().anyMatch(RoleAssignment::isGlobal));
        assertTrue(assignments.stream().anyMatch(a -> !a.isGlobal() && "IT".equals(a.getDepartment().getName())));
        assertTrue(assignments.stream().allMatch(a -> a.getRole().getPermissions().size() == 1));
    }

    @Test
    void sameDepartmentGrantTwiceViolatesUniqueConstraint() {
        grant(it);
        entityManager.flush();

        RoleAssignment duplicate = RoleAssignment.builder().user(user).role(role).department(it).build();
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
    }

    @Test
    void existsQueriesDistinguishGlobalFromDepartmentScoped() {
        grant(null);
        entityManager.flush();

        assertTrue(repository.existsByUserIdAndRoleIdAndDepartmentIsNull(user.getId(), role.getId()));
        assertFalse(repository.existsByUserIdAndRoleIdAndDepartmentId(user.getId(), role.getId(), it.getId()));
    }

    @Test
    void deletingTheRoleRemovesItsAssignments() {
        grant(it);
        entityManager.flush();
        entityManager.clear();

        roleRepository.deleteById(role.getId());
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, countAssignments());
        assertTrue(userRepository.findById(user.getId()).isPresent(), "the user itself survives");
    }

    @Test
    void deletingTheUserRemovesItsAssignments() {
        grant(it);
        entityManager.flush();
        entityManager.clear();

        userRepository.deleteById(user.getId());
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, countAssignments());
        assertTrue(roleRepository.findById(role.getId()).isPresent(), "the role itself survives");
    }

    @Test
    void allForeignKeysAreDeclaredOnDeleteCascade() {
        assertEquals("CASCADE", deleteRuleFor("USER_ID"));
        assertEquals("CASCADE", deleteRuleFor("ROLE_ID"));
        assertEquals("CASCADE", deleteRuleFor("DEPARTMENT_ID"));
    }

    private long countAssignments() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM role_assignments").getSingleResult())
            .longValue();
    }

    private String deleteRuleFor(String column) {
        return (String) entityManager.createNativeQuery("""
                SELECT rc.DELETE_RULE
                FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                  ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
                 AND kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
                WHERE UPPER(kcu.TABLE_NAME) = 'ROLE_ASSIGNMENTS'
                  AND UPPER(kcu.COLUMN_NAME) = :column
                """)
            .setParameter("column", column)
            .getSingleResult();
    }
}
