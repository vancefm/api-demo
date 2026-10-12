package com.demo.feature.security.rbac.role;

import com.demo.platform.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.HashSet;
import java.util.Set;

/**
 * A named set of {@link Permission}s.
 *
 * <p>Roles carry no notion of <em>who</em> holds them or <em>where</em>: that is
 * the {@code RoleAssignment} (user, role, department). The same role can be
 * granted to many users across many departments.
 *
 * <p>{@code system} roles are seeded at startup (currently only
 * {@code SuperAdmin}) and cannot be renamed, deleted, or have their permissions
 * changed through the API — losing the wildcard role would lock everyone out.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "system_role", nullable = false)
    @Builder.Default
    private boolean system = false;

    /**
     * The grants this role carries, held as values rather than entities (see
     * {@link Permission}). Hibernate writes them to the {@code role_permissions}
     * collection table and rewrites the whole collection when it changes, so
     * replacing a role's permissions needs no diffing and cannot trip the
     * unique constraint. {@link OnDelete} makes the database drop the rows with
     * the role, and {@link BatchSize} keeps a page of roles at a bounded number
     * of queries (the fetching rule in {@code UserRepository}).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_role_permission",
            columnNames = {"role_id", "entity_name", "field_name", "operation"}))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Adds a grant, returning it. A duplicate is silently absorbed by the set.
     */
    public Permission addPermission(String entity, String field, Operation operation) {
        Permission permission = Permission.builder()
            .entity(entity)
            .field(field)
            .operation(operation)
            .build();
        permissions.add(permission);
        return permission;
    }
}
