package com.demo.feature.security.rbac;

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
     * Owned collection: adding/removing here is flushed with the role (no
     * repository for {@link Permission}). Batched, not graphed, on paged reads —
     * see the fetching rule in {@code UserRepository}.
     */
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    public Permission addPermission(String entity, String field, Operation operation) {
        Permission permission = Permission.builder()
            .role(this)
            .entity(entity)
            .field(field)
            .operation(operation)
            .build();
        permissions.add(permission);
        return permission;
    }
}
