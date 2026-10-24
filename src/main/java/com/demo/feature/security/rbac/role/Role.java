package com.demo.feature.security.rbac.role;

import com.demo.platform.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.Collection;
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
     * The grants this role carries, owned outright: {@code orphanRemoval}
     * deletes a grant dropped from the set, and the {@code ON DELETE CASCADE} on
     * {@link Permission#getRole()} makes the database drop them with the role.
     * {@link BatchSize} keeps a page of roles at a bounded number of queries (the
     * fetching rule in {@code UserRepository}). Change it through
     * {@link #addPermission} and {@link #replacePermissions}, which set the back
     * reference; a grant added to the set directly has no {@code role_id}.
     */
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Adds a grant, returning the one the role now holds: an equal grant
     * already present is kept and returned rather than duplicated.
     */
    public Permission addPermission(String entity, String field, Operation operation) {
        Permission permission = Permission.builder()
            .entity(entity)
            .field(field)
            .operation(operation)
            .build();
        for (Permission existing : permissions) {
            if (existing.equals(permission)) {
                return existing;
            }
        }
        permission.setRole(this);
        permissions.add(permission);
        return permission;
    }

    /**
     * Makes the role's grants exactly {@code desired}, matched by business key.
     * Retain-then-add rather than clear-and-add: surviving grants keep their row
     * and id, so only genuine removals are deleted and only genuine additions
     * inserted. Clear-and-add would re-insert every survivor, and because
     * Hibernate flushes inserts before deletes those inserts hit
     * {@code uk_role_permission}.
     */
    public void replacePermissions(Collection<Permission> desired) {
        permissions.retainAll(desired);
        desired.forEach(p -> addPermission(p.getEntity(), p.getField(), p.getOperation()));
    }
}
