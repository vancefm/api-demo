package com.demo.feature.security.rbac.role;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One {@code entity:field → operation} grant belonging to a {@link Role}.
 *
 * <p>{@code entity} is the secured entity's name (e.g. {@code User}) and
 * {@code field} one of its DTO property names (e.g. {@code firstName}); either
 * may be {@link #ANY} to mean "every entity" / "every field".
 *
 * <p>This is a <strong>value</strong>, not an entity: a permission has no
 * identity of its own, is never edited in place, and is meaningless apart from
 * the role that holds it. It is therefore mapped as an {@link Embeddable} in
 * {@code Role.permissions}, which Hibernate stores in the {@code role_permissions}
 * collection table. Two permissions with the same entity, field and operation
 * are the same permission — hence {@link EqualsAndHashCode}, which is what lets
 * the owning {@code Set} collapse duplicates before they ever reach the
 * database.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Permission {

    /**
     * Wildcard for {@code entity} or {@code field}.
     */
    public static final String ANY = "*";

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
