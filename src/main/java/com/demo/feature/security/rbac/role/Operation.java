package com.demo.feature.security.rbac.role;

/**
 * The four things a permission can allow on an entity (or one of its fields).
 *
 * <p>{@link #CREATE}, {@link #READ} and {@link #UPDATE} may be granted per
 * field; {@link #DELETE} is only meaningful for the whole entity, so a DELETE
 * permission's field is always the wildcard.
 */
public enum Operation {
    CREATE,
    READ,
    UPDATE,
    DELETE
}
