package com.demo.feature.security.rbac.access;
import com.demo.feature.security.rbac.role.Permission;

import com.demo.platform.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * All {@link SecuredEntity} beans, by name.
 *
 * <p>Adding a new domain model to the RBAC layer means declaring one more
 * {@code SecuredEntity} bean in that feature's package — nothing here changes.
 * The registry is what makes permission rows trustworthy: a permission naming
 * an unknown entity or field is rejected at write time instead of silently
 * never matching anything.
 */
@Component
public class SecuredEntityRegistry {

    private final Map<String, SecuredEntity<?>> byName;

    public SecuredEntityRegistry(List<SecuredEntity<?>> entities) {
        this.byName = entities.stream()
            .collect(Collectors.toMap(SecuredEntity::name, Function.identity(), (a, b) -> {
                throw new IllegalStateException("Two secured entities are named '" + a.name() + "'");
            }, TreeMap::new));
    }

    public Optional<SecuredEntity<?>> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public SecuredEntity<?> require(String name) {
        return find(name).orElseThrow(() ->
            new InvalidRequestException("Unknown secured entity '" + name + "'; known entities: " + byName.keySet()));
    }

    /**
     * Rejects a permission target that does not exist. Wildcards are always
     * legal; a wildcard entity only pairs with a wildcard field.
     */
    public void requireKnown(String entity, String field) {
        if (Permission.ANY.equals(entity)) {
            if (!Permission.ANY.equals(field)) {
                throw new InvalidRequestException("A permission on every entity ('*') must use field '*'");
            }
            return;
        }
        SecuredEntity<?> secured = require(entity);
        if (!Permission.ANY.equals(field) && !secured.fieldNames().contains(field)) {
            throw new InvalidRequestException("Unknown field '" + field + "' on entity '" + entity
                + "'; known fields: " + secured.fieldNames().stream().sorted().toList());
        }
    }

    public Iterable<String> names() {
        return byName.keySet();
    }
}
