package com.demo.domain;

import java.util.Set;

/**
 * Marker for domain objects whose access is scoped by ownership and/or department
 * membership. Enables the authorization layer (department intersection + OWN checks)
 * to operate generically over any departmented entity, current or future.
 */
public interface ScopedResource {

    /**
     * Identifier of the principal that "owns" this resource for {@code OWN}-scoped access,
     * or {@code null} when the resource has no owner. For a computer system this is its
     * assigned user; for a user it is the user's own id.
     */
    Long getOwnerId();

    /**
     * Ids of the departments this resource belongs to. {@code DEPARTMENT}-scoped access is
     * granted when these intersect the acting user's own department designation.
     */
    Set<Long> getDepartmentIds();
}
