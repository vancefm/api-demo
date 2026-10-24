package com.demo.feature.security.rbac.role;

import com.demo.platform.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The role/permission mapping against a real schema.
 *
 * <p>Permissions are entities compared by business key, so what matters here is
 * that they round-trip as equal grants, that the whole set can be replaced in
 * one transaction without tripping the unique constraint (survivors keep their
 * rows), and that the database drops the rows with the role.
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
    void savesPermissionsWithTheRoleAndReloadsThemAsEqualGrants() {
        Role role = newRole("Reader");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "lastName", Operation.READ);
        Long id = repository.saveAndFlush(role).getId();
        entityManager.clear();

        Role reloaded = repository.findById(id).orElseThrow();

        assertEquals(2, reloaded.getPermissions().size());
        assertFalse(reloaded.isSystem());
        // Business-key equality: a reloaded grant equals a freshly built one
        assertTrue(reloaded.getPermissions().contains(
            Permission.builder().entity("User").field("firstName").operation(Operation.READ).build()));
    }

    /**
     * The duplicate never reaches the database: equal grants collapse in the
     * owning {@code Set}. The unique constraint on the table is the backstop,
     * asserted separately.
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

        // Same grant resubmitted alongside a new one: a clear-and-add would
        // re-insert it before deleting the old row.
        role.replacePermissions(List.of(
            grant("firstName", Operation.READ),
            grant("email", Operation.UPDATE)));
        repository.saveAndFlush(role);
        entityManager.clear();

        assertEquals(2, countPermissions());
        assertEquals(2, repository.findById(role.getId()).orElseThrow().getPermissions().size());
    }

    @Test
    void replacingPermissions_keepsTheIdsOfSurvivingGrants() {
        Role role = newRole("Stable");
        role.addPermission("User", "firstName", Operation.READ);
        role.addPermission("User", "lastName", Operation.READ);
        role = repository.saveAndFlush(role);
        Long survivorId = idOf(role, "lastName");

        role.replacePermissions(List.of(
            grant("lastName", Operation.READ),
            grant("email", Operation.READ)));
        repository.flush();
        entityManager.clear();

        Role reloaded = repository.findById(role.getId()).orElseThrow();
        assertEquals(survivorId, idOf(reloaded, "lastName"));
        assertNotNull(idOf(reloaded, "email"));
        assertTrue(reloaded.getPermissions().stream().noneMatch(p -> "firstName".equals(p.getField())));
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
    void permissionTableHasTheUniqueConstraintAsABackstop() {
        Object count = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE UPPER(TABLE_NAME) = 'ROLE_PERMISSIONS'
                  AND CONSTRAINT_TYPE = 'UNIQUE'
                  AND UPPER(CONSTRAINT_NAME) = 'UK_ROLE_PERMISSION'
                """).getSingleResult();

        assertEquals(1L, ((Number) count).longValue());
    }

    private static Permission grant(String field, Operation operation) {
        return Permission.builder().entity("User").field(field).operation(operation).build();
    }

    private static Long idOf(Role role, String field) {
        return role.getPermissions().stream()
            .filter(p -> field.equals(p.getField()))
            .findFirst()
            .orElseThrow()
            .getId();
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
