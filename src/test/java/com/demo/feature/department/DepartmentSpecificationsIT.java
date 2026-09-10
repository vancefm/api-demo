package com.demo.feature.department;

import com.demo.feature.user.User;
import com.demo.feature.user.UserDepartment;
import com.demo.feature.user.UserRepository;
import com.demo.platform.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code assignedToAnyDepartment} is the predicate the RBAC layer appends to
 * paged reads, so it must count an owner once regardless of how many of the
 * departments it belongs to, and must match nothing for an empty set.
 */
@DataJpaTest
@Import(JpaConfig.class)
class DepartmentSpecificationsIT {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    private Department it;
    private Department hr;
    private Department ops;

    @BeforeEach
    void setUp() {
        it = departmentRepository.save(Department.builder().name("IT").build());
        hr = departmentRepository.save(Department.builder().name("HR").build());
        ops = departmentRepository.save(Department.builder().name("OPS").build());

        saveUser("both", it, hr);
        saveUser("hr.only", hr);
        saveUser("ops.only", ops);
        saveUser("nowhere");
    }

    private void saveUser(String username, Department... departments) {
        User user = User.builder().username(username).email(username + "@example.com").build();
        for (Department department : departments) {
            user.getDepartmentLinks().add(UserDepartment.builder().user(user).department(department).build());
        }
        userRepository.save(user);
    }

    @Test
    void ownerInSeveralMatchingDepartmentsIsCountedOnce() {
        Page<User> page = userRepository.findAll(
            DepartmentSpecifications.assignedToAnyDepartment(Set.of(it.getId(), hr.getId())), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertEquals(Set.of("both", "hr.only"),
            Set.copyOf(page.getContent().stream().map(User::getUsername).toList()));
    }

    @Test
    void singleDepartmentMatchesItsMembersOnly() {
        Page<User> page = userRepository.findAll(
            DepartmentSpecifications.assignedToAnyDepartment(Set.of(it.getId())), PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("both", page.getContent().get(0).getUsername());
    }

    @Test
    void emptySetMatchesNothing() {
        Page<User> page = userRepository.findAll(
            DepartmentSpecifications.assignedToAnyDepartment(Set.of()), PageRequest.of(0, 10));

        assertEquals(0, page.getTotalElements());
    }
}
