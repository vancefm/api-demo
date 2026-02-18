package com.demo.application.user;

import com.demo.application.security.auth.RoleRepository;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.User;
import com.demo.shared.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@WithMockUser(username = "admin")
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role testRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        testRole = roleRepository.save(Role.builder()
                .name("MY_APP_USER")
                .description("Test role")
                .build());

        testUser = User.builder()
                .username("john.doe")
                .email("john.doe@example.com")
                .department("IT")
                .role(testRole)
                .build();
    }

    @Test
    void testSaveUser() {
        User saved = userRepository.save(testUser);

        assertNotNull(saved.getId());
        assertEquals("john.doe", saved.getUsername());
        assertEquals("john.doe@example.com", saved.getEmail());
        assertEquals("IT", saved.getDepartment());
    }

    @Test
    void testFindById() {
        User saved = userRepository.save(testUser);

        Optional<User> found = userRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("john.doe", found.get().getUsername());
    }

    @Test
    void testFindByUsername() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByUsername("john.doe");

        assertTrue(found.isPresent());
        assertEquals("john.doe", found.get().getUsername());
    }

    @Test
    void testFindByUsername_NotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertTrue(found.isEmpty());
    }

    @Test
    void testExistsByUsername() {
        userRepository.save(testUser);

        assertTrue(userRepository.existsByUsername("john.doe"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    void testExistsByEmail() {
        userRepository.save(testUser);

        assertTrue(userRepository.existsByEmail("john.doe@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void testFindAll() {
        userRepository.save(testUser);

        User secondUser = User.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .department("HR")
                .role(testRole)
                .build();
        userRepository.save(secondUser);

        var users = userRepository.findAll();

        assertEquals(2, users.size());
    }

    @Test
    void testUpdateUser() {
        User saved = userRepository.save(testUser);
        saved.setEmail("john.updated@example.com");
        saved.setDepartment("Engineering");
        userRepository.save(saved);

        User updated = userRepository.findById(saved.getId()).orElseThrow();

        assertEquals("john.updated@example.com", updated.getEmail());
        assertEquals("Engineering", updated.getDepartment());
    }

    @Test
    void testDeleteUser() {
        User saved = userRepository.save(testUser);
        Long id = saved.getId();

        userRepository.deleteById(id);

        Optional<User> found = userRepository.findById(id);
        assertTrue(found.isEmpty());
    }

    @Test
    void testUniqueUsernameConstraint() {
        userRepository.save(testUser);

        User duplicate = User.builder()
                .username("john.doe")
                .email("different@example.com")
                .department("HR")
                .role(testRole)
                .build();

        assertThrows(Exception.class, () -> {
            userRepository.save(duplicate);
            userRepository.flush();
        });
    }

    @Test
    void testUserWithManager() {
        User manager = userRepository.save(User.builder()
                .username("manager")
                .email("manager@example.com")
                .department("IT")
                .role(testRole)
                .build());

        testUser.setManager(manager);
        User saved = userRepository.save(testUser);

        User found = userRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(found.getManager());
        assertEquals("manager", found.getManager().getUsername());
    }
}
