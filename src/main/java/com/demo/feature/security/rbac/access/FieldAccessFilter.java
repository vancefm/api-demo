package com.demo.feature.security.rbac.access;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Set;

/**
 * Masks the properties of a DTO the caller may not read.
 *
 * <p>Unreadable properties are set to {@code null}; the DTOs carry
 * {@code @JsonInclude(NON_NULL)}, so those keys are <em>omitted</em> from the
 * JSON rather than rendered as null. {@code id} is always kept — a response
 * that cannot be addressed is useless. Primitive-typed properties cannot be
 * nulled and are left alone; secured DTOs use wrapper types.
 */
public final class FieldAccessFilter {

    private static final String ID = "id";

    private FieldAccessFilter() {
    }

    /**
     * Nulls every writable property of {@code dto} that is not in
     * {@code readableFields} (except {@code id}); returns the same instance.
     */
    public static <T> T retainOnly(T dto, Set<String> readableFields) {
        if (dto == null) {
            return null;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(dto);
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(dto.getClass())) {
            String name = descriptor.getName();
            if (descriptor.getWriteMethod() == null
                || ID.equals(name)
                || readableFields.contains(name)
                || descriptor.getPropertyType().isPrimitive()) {
                continue;
            }
            wrapper.setPropertyValue(name, null);
        }
        return dto;
    }
}
