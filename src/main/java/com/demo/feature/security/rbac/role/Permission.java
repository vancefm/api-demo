package com.demo.feature.security.rbac.role;

import com.demo.platform.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One {@code entity:field → operation} grant belonging to a {@link Role}.
 *
 * <p>{@code entity} is the secured entity's name (e.g. {@code User}) and
 * {@code field} one of its DTO property names (e.g. {@code firstName}); either
 * may be {@link #ANY} to mean "every entity" / "every field". Rows are owned by
 * the role's {@code permissions} collection ({@code cascade = ALL},
 * {@code orphanRemoval = true}), so they need no repository of their own, and
 * the foreign key is {@code ON DELETE CASCADE} so deleting a role removes them
 * at the database level too.
 */
@Entity
@Table(name = "permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_permission",
        columnNames = {"role_id", "entity_name", "field_name", "operation"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Permission extends BaseEntity {

    /**
     * Wildcard for {@code entity} or {@code field}.
     */
    public static final String ANY = "*";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Role role;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entity;

    @Column(name = "field_name", nullable = false, length = 100)
    private String field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Operation operation;

    /**
     * Whether this grant applies to the given entity/field/operation triple.
     */
    public boolean covers(String entityName, String fieldName, Operation op) {
        return operation == op
            && (ANY.equals(entity) || entity.equals(entityName))
            && (ANY.equals(field) || field.equals(fieldName));
    }

    /**
     * Identity of the grant independent of its database id — used to diff a
     * requested permission list against the stored one.
     */
    public String key() {
        return entity + ":" + field + ":" + operation;
    }
}
