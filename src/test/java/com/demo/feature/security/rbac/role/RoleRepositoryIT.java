package com.demo.feature.security.rbac.role;

import com.demo.platform.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The role/permission mapping against a real schema: cascade on save, orphan
 * removal, the unique grant constraint, and the database-level cascade that
 * removes permissions when a role row is deleted.
 */
@DataJpaTest
@Import(JpaConfig.class)
class RoleRepositoryIT {

    @Autowired
    private RoleRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Role newRole(String name) {
        return Role.builder().name(name).description("test").build();
    }

    @Test
    void savesPermissionsWithTheRoleAndReloadsThem() {
        Role role = newRole("Reader");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "lastName", Operation.READ);
        Long id = repository.saveAndFlush(role).getId();
        entityManager.clear();

        Role reloaded = repository.findById(id).orElseThrow();

        assertEquals(2, reloaded.getPermissions().size());
        assertFalse(reloaded.isSystem());
        assertTrue(reloaded.getPermissions().stream().allMatch(p -> p.getId() != null));
    }

    @Test
    void sameGrantTwiceOnOneRoleViolatesUniqueConstraint() {
        Role role = newRole("Duplicated");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "firstName", Operation.READ);

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(role));
    }

    @Test
    void sameGrantOnDifferentRolesIsFine() {
        Role a = newRole("A");
        a.addPermission("User", "firstName", Operation.READ);
        Role b = newRole("B");
        b.addPermission("User", "firstName", Operation.READ);

        repository.saveAndFlush(a);
        repository.saveAndFlush(b);

        assertEquals(2, countPermissions());
    }

    @Test
    void removingFromTheCollectionDeletesTheRow() {
        Role role = newRole("Shrinking");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "lastName", Operation.READ);
        role = repository.saveAndFlush(role);

        role.getPermissions().removeIf(p -> "lastName".equals(p.getField()));
        repository.saveAndFlush(role);
        entityManager.clear();

        assertEquals(1, countPermissions());
        assertEquals(1, repository.findById(role.getId()).orElseThrow().getPermissions().size());
    }

    @Test
    void deletingTheRoleRemovesItsPermissions() {
        Role role = newRole("Doomed");
        role.addPermission("*", "*", Operation.READ);
        role = repository.saveAndFlush(role);
        entityManager.clear();

        repository.deleteById(role.getId());
        entityManager.flush();

        assertEquals(0, countPermissions());
    }

    @Test
    void permissionForeignKeyIsDeclaredOnDeleteCascade() {
        assertEquals("CASCADE", deleteRuleFor("permissions", "ROLE_ID"));
    }

    private long countPermissions() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM permissions").getSingleResult())
            .longValue();
    }

    private String deleteRuleFor(String table, String column) {
        return (String) entityManager.createNativeQuery("""
                SELECT rc.DELETE_RULE
                FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                  ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
                 AND kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
                WHERE UPPER(kcu.TABLE_NAME) = UPPER(:table)
                  AND UPPER(kcu.COLUMN_NAME) = :column
                """)
            .setParameter("table", table)
            .setParameter("column", column)
            .getSingleResult();
    }
}
