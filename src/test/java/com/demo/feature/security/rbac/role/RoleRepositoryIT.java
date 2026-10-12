package com.demo.feature.security.rbac.role;

import com.demo.platform.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The role/permission mapping against a real schema.
 *
 * <p>Permissions are values in a collection table, so what matters here is that
 * they round-trip as equal values, that the whole set can be rewritten in one
 * transaction without tripping the unique constraint, and that the database
 * drops the rows with the role.
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
    void savesPermissionsWithTheRoleAndReloadsThemAsEqualValues() {
        Role role = newRole("Reader");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "lastName", Operation.READ);
        Long id = repository.saveAndFlush(role).getId();
        entityManager.clear();

        Role reloaded = repository.findById(id).orElseThrow();

        assertEquals(2, reloaded.getPermissions().size());
        assertFalse(reloaded.isSystem());
        // Values, not entities: a reloaded grant equals a freshly built one
        assertTrue(reloaded.getPermissions().contains(
            Permission.builder().entity("User").field("firstName").operation(Operation.READ).build()));
    }

    /**
     * The duplicate never reaches the database: equal grants collapse in the
     * owning {@code Set}. The unique constraint on the collection table is the
     * backstop, asserted separately.
     */
    @Test
    void sameGrantTwiceOnOneRoleCollapses() {
        Role role = newRole("Duplicated");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "firstName", Operation.READ);

        repository.saveAndFlush(role);

        assertEquals(1, countPermissions());
    }

    @Test
    void replacingTheWholeSetInOneTransactionDoesNotTripTheUniqueConstraint() {
        Role role = newRole("Rewritten");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "lastName", Operation.READ);
        role = repository.saveAndFlush(role);

        // Same grant resubmitted alongside a new one — the case that forced a
        // hand-written diff when permissions were entities.
        role.getPermissions().clear();
        role.getPermissions().add(
            Permission.builder().entity("User").field("firstName").operation(Operation.READ).build());
        role.getPermissions().add(
            Permission.builder().entity("User").field("email").operation(Operation.UPDATE).build());
        repository.saveAndFlush(role);
        entityManager.clear();

        assertEquals(2, countPermissions());
        assertEquals(2, repository.findById(role.getId()).orElseThrow().getPermissions().size());
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
        assertEquals("CASCADE", deleteRuleFor("role_permissions", "ROLE_ID"));
    }

    @Test
    void collectionTableHasTheUniqueConstraintAsABackstop() {
        Object count = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE UPPER(TABLE_NAME) = 'ROLE_PERMISSIONS'
                  AND CONSTRAINT_TYPE IN ('UNIQUE', 'PRIMARY KEY')
                """).getSingleResult();

        assertEquals(1L, ((Number) count).longValue());
    }

    private long countPermissions() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM role_permissions").getSingleResult())
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
