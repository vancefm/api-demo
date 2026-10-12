package com.demo.feature.security.rbac.access;

import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Describes one kind of thing the RBAC layer can protect.
 *
 * <p>Each feature declares a bean of this type for its DTO (see
 * {@code UserSecuredEntity}); {@link SecuredEntityRegistry} collects them. The
 * description carries the three facts access control needs:
 * <ul>
 *   <li>{@code name} — what permissions refer to (e.g. {@code "User"}),</li>
 *   <li>{@code dtoClass} — whose property names are the permissible field
 *       names (clients grant what clients see: the JSON property names),</li>
 *   <li>{@code departmentIdsOf} — which departments a given DTO belongs to,
 *       i.e. which department-scoped grants apply to it. An empty set means the
 *       object is not departmental and only global grants apply.</li>
 * </ul>
 *
 * @param <T> the DTO type
 */
public record SecuredEntity<T>(String name, Class<T> dtoClass, Function<T, Set<Long>> departmentIdsOf) {

    /**
     * Convenience for DTOs that expose {@code List<Long> departmentIds}.
     */
    public static <T> SecuredEntity<T> departmental(String name, Class<T> dtoClass,
                                                    Function<T, Collection<Long>> departmentIds) {
        return new SecuredEntity<>(name, dtoClass, dto -> {
            Collection<Long> ids = departmentIds.apply(dto);
            return ids == null ? Set.of() : new HashSet<>(ids);
        });
    }

    /**
     * Convenience for DTOs that never belong to a department (global-only).
     */
    public static <T> SecuredEntity<T> global(String name, Class<T> dtoClass) {
        return new SecuredEntity<>(name, dtoClass, dto -> Set.of());
    }

    /**
     * Departments the given DTO belongs to. The argument is typed as
     * {@code Object} so callers holding a {@code SecuredEntity<?>} can use it.
     */
    public Set<Long> departmentIds(Object dto) {
        return departmentIdsOf.apply(dtoClass.cast(dto));
    }

    /**
     * Property names of the DTO — the set of legal field names for permissions
     * on this entity.
     */
    public Set<String> fieldNames() {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(dtoClass))
            .map(PropertyDescriptor::getName)
            .filter(property -> !"class".equals(property))
            .collect(Collectors.toSet());
    }
}
