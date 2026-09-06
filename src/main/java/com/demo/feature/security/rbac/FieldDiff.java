package com.demo.feature.security.rbac;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Field-level reasoning over DTOs, by property name.
 *
 * <p>Works on the DTO's <em>editable</em> properties: everything with a setter
 * except {@code id} and properties whose field is marked
 * {@code @Schema(accessMode = READ_ONLY)} (derived data such as
 * {@code departments} or {@code roleAssignments}, which a client echoes back
 * but never writes). Null and empty collections are treated as the same value,
 * matching the service semantics where both clear an association.
 */
public final class FieldDiff {

    private static final String ID = "id";

    private FieldDiff() {
    }

    /**
     * Property names a client can write on this DTO type.
     */
    public static Set<String> editableFields(Class<?> dtoClass) {
        Set<String> editable = new LinkedHashSet<>();
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(dtoClass)) {
            String name = descriptor.getName();
            if (descriptor.getWriteMethod() == null || ID.equals(name) || isReadOnly(dtoClass, name)) {
                continue;
            }
            editable.add(name);
        }
        return editable;
    }

    /**
     * Editable properties whose value differs between the stored and incoming
     * representations — what an update actually writes.
     */
    public static Set<String> changedFields(Object stored, Object incoming) {
        BeanWrapper before = new BeanWrapperImpl(stored);
        BeanWrapper after = new BeanWrapperImpl(incoming);
        Set<String> changed = new LinkedHashSet<>();
        for (String field : editableFields(incoming.getClass())) {
            if (!Objects.equals(normalize(before.getPropertyValue(field)), normalize(after.getPropertyValue(field)))) {
                changed.add(field);
            }
        }
        return changed;
    }

    /**
     * Editable properties the client actually supplied (non-null, non-empty) —
     * what a create actually writes.
     */
    public static Set<String> suppliedFields(Object dto) {
        BeanWrapper wrapper = new BeanWrapperImpl(dto);
        Set<String> supplied = new LinkedHashSet<>();
        for (String field : editableFields(dto.getClass())) {
            if (normalize(wrapper.getPropertyValue(field)) != null) {
                supplied.add(field);
            }
        }
        return supplied;
    }

    /**
     * Copies the stored value of every editable property the caller cannot read
     * into {@code incoming}. A caller never saw those fields, so whatever the
     * request carries for them (typically null) is not a change; this keeps a
     * GET-then-PUT round trip from a narrow role honest and lossless.
     */
    public static void retainUnreadable(Object stored, Object incoming, Set<String> readableFields) {
        BeanWrapper before = new BeanWrapperImpl(stored);
        BeanWrapper after = new BeanWrapperImpl(incoming);
        for (String field : editableFields(incoming.getClass())) {
            if (!readableFields.contains(field)) {
                after.setPropertyValue(field, before.getPropertyValue(field));
            }
        }
    }

    static boolean isReadOnly(Class<?> dtoClass, String property) {
        Field field = ReflectionUtils.findField(dtoClass, property);
        if (field == null) {
            return false;
        }
        Schema schema = field.getAnnotation(Schema.class);
        return schema != null && schema.accessMode() == Schema.AccessMode.READ_ONLY;
    }

    private static Object normalize(Object value) {
        if (value instanceof Collection<?> collection && collection.isEmpty()) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        return value;
    }
}
