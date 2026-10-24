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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
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
 * may be {@link #ANY} to mean "every entity" / "every field".
 *
 * <p>An entity with a surrogate id, but <strong>equality is the business key</strong>
 * (entity, field, operation), never the id. Within one role's {@code Set} that
 * is what makes a grant unique, so duplicates collapse before they reach the
 * database, and a freshly built grant finds the stored row it matches. That
 * lookup is what lets {@link Role#replacePermissions} keep surviving rows
 * instead of re-inserting them: Hibernate flushes inserts before deletes, so a
 * clear-and-add would trip {@code uk_role_permission} on any grant resubmitted
 * unchanged. The key fields have no setters, so a grant's hash cannot change
 * while it sits in the set.
 */
@Entity
@Table(name = "role_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_role_permission",
        columnNames = {"role_id", "entity_name", "field_name", "operation"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Permission extends BaseEntity {

    /**
     * Wildcard for {@code entity} or {@code field}.
     */
    public static final String ANY = "*";

    /**
     * Set only by {@link Role#addPermission}, so a grant cannot join one role's
     * set while pointing at another.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter(AccessLevel.PACKAGE)
    private Role role;

    @EqualsAndHashCode.Include
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entity;

    @EqualsAndHashCode.Include
    @Column(name = "field_name", nullable = false, length = 100)
    private String field;

    @EqualsAndHashCode.Include
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
     * Human-readable form, used in log and error messages and as a stable sort
     * key. Carries the same information as {@link #equals}.
     */
    public String key() {
        return entity + ":" + field + ":" + operation;
    }

    @Override
    public String toString() {
        return key();
    }
}
