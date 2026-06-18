package com.demo.shared.security;

import com.demo.domain.ScopedResource;
import com.demo.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Applies field-level read/write policy (resolved by {@link GrantService}) to DTOs.
 *
 * <p>Reads: fields the user may not read are nulled on the typed DTO (preserving the API
 * contract). Writes: any attempt to change a non-writable field is rejected with 403.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FieldProjectionService {

    /** Always-visible identity/derived fields, never stripped or treated as writable. */
    private static final Set<String> ALWAYS_READABLE = Set.of("id");
    private static final Set<String> DERIVED_FIELDS = Set.of("id", "departmentNames", "roleNames");

    private final GrantService grantService;
    private final ObjectMapper objectMapper;

    /** Null out fields the user may not read on this resource; returns the same DTO. */
    public <T> T filterReadable(User user, ScopedResource resource, T dto, String resourceType) {
        Optional<Set<String>> allowed = grantService.readableFields(user, resource, resourceType);
        if (allowed.isEmpty()) {
            return dto; // no restriction — full field set
        }
        Set<String> keep = new HashSet<>(allowed.get());
        keep.addAll(ALWAYS_READABLE);
        for (Field field : dto.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || keep.contains(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(dto, null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to filter field " + field.getName(), e);
            }
        }
        return dto;
    }

    /**
     * Reject the update if it changes any field the user is not permitted to write.
     * {@code existingDto} is the persisted state, {@code incomingDto} the requested change.
     */
    public void validateWritable(User user, ScopedResource existing, Object existingDto,
                                 Object incomingDto, String resourceType) {
        Optional<Set<String>> writable = grantService.writableFields(user, existing, resourceType);
        if (writable.isEmpty()) {
            return; // no restriction — all fields writable
        }
        Map<String, Object> before = toMap(existingDto);
        Map<String, Object> after = toMap(incomingDto);
        for (Map.Entry<String, Object> entry : after.entrySet()) {
            String field = entry.getKey();
            if (DERIVED_FIELDS.contains(field)) {
                continue;
            }
            if (changed(before.get(field), entry.getValue()) && !writable.get().contains(field)) {
                throw new AccessDeniedException("Not permitted to modify field: " + field);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object dto) {
        return objectMapper.convertValue(dto, Map.class);
    }

    /** Order-insensitive comparison so collection fields (e.g. departmentIds) don't false-positive. */
    private boolean changed(Object before, Object after) {
        if (before instanceof Collection<?> b && after instanceof Collection<?> a) {
            return !new HashSet<>(b).equals(new HashSet<>(a));
        }
        return !Objects.equals(before, after);
    }
}
