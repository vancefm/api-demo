package com.demo.application.security;

import com.demo.application.security.config.ActiveDirectoryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Active Directory group-to-role mapping logic that is applied
 * by {@link com.demo.application.security.config.ActiveDirectoryConfig}.
 *
 * <p>Tests verify that {@link ActiveDirectoryProperties#getRoleToAdGroups()} is
 * correctly inverted into a case-insensitive groupCN → ROLE_* lookup at startup,
 * and that the authorities mapper applies the mapping (including the fallback role)
 * as expected.</p>
 */
class ActiveDirectoryRoleMappingTest {

    /**
     * Builds the normalized groupCN → role lookup from a {@link ActiveDirectoryProperties}
     * instance, mirroring the logic in {@code ActiveDirectoryConfig}.
     */
    private Map<String, String> buildNormalizedMapping(ActiveDirectoryProperties props) {
        Map<String, String> normalized = new HashMap<>();
        if (props.getRoleToAdGroups() == null) {
            return normalized;
        }
        for (Map.Entry<String, List<String>> entry : props.getRoleToAdGroups().entrySet()) {
            String roleValue = entry.getKey();
            if (!roleValue.startsWith("ROLE_")) {
                roleValue = "ROLE_" + roleValue;
            }
            List<String> groups = entry.getValue();
            if (groups == null) {
                continue;
            }
            for (String groupCn : groups) {
                if (StringUtils.hasText(groupCn)) {
                    normalized.put(groupCn.toUpperCase(), roleValue);
                }
            }
        }
        return Collections.unmodifiableMap(normalized);
    }

    /**
     * Simulates the authorities mapper lambda from {@code ActiveDirectoryConfig}.
     */
    private List<GrantedAuthority> mapAuthorities(
            List<GrantedAuthority> authorities, Map<String, String> groupRoleMapping) {
        if (authorities == null || authorities.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_MY_APP_USER"));
        }
        Set<GrantedAuthority> mapped = new HashSet<>();
        for (GrantedAuthority authority : authorities) {
            String name = authority.getAuthority().toUpperCase();
            if (name.startsWith("ROLE_")) {
                name = name.substring(5);
            }
            String mappedRole = groupRoleMapping.get(name);
            if (mappedRole != null) {
                mapped.add(new SimpleGrantedAuthority(mappedRole));
            }
        }
        if (mapped.isEmpty()) {
            mapped.add(new SimpleGrantedAuthority("ROLE_MY_APP_USER"));
        }
        return new ArrayList<>(mapped);
    }

    private Set<String> toRoleSet(List<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------
    // Property defaults
    // ------------------------------------------------------------------

    @Test
    void roleToAdGroupsDefaultsToEmpty() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        assertNotNull(props.getRoleToAdGroups());
        assertTrue(props.getRoleToAdGroups().isEmpty(),
                "roleToAdGroups should be empty by default – no hard-coded mappings");
    }

    // ------------------------------------------------------------------
    // Normalised mapping construction
    // ------------------------------------------------------------------

    @Test
    void normalizedMappingAddsRolePrefix() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of("MY_APP_USER", List.of("GroupA-Users")));

        Map<String, String> mapping = buildNormalizedMapping(props);

        assertEquals("ROLE_MY_APP_USER", mapping.get("GROUPA-USERS"));
    }

    @Test
    void normalizedMappingPreservesExistingRolePrefix() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of("ROLE_MY_APP_ADMIN", List.of("GroupB-Admins")));

        Map<String, String> mapping = buildNormalizedMapping(props);

        assertEquals("ROLE_MY_APP_ADMIN", mapping.get("GROUPB-ADMINS"),
                "Existing ROLE_ prefix should not be doubled");
    }

    @Test
    void normalizedMappingIsCaseInsensitive() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of("MY_APP_ADMIN", List.of("groupb-admins")));

        Map<String, String> mapping = buildNormalizedMapping(props);

        // Lookup key is uppercased, so a mixed-case group CN must be found
        assertEquals("ROLE_MY_APP_ADMIN", mapping.get("GROUPB-ADMINS"));
    }

    @Test
    void normalizedMappingMultipleGroupsForOneRole() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of(
                "MY_APP_ADMIN", List.of("GroupB-Admins", "GroupB-Helpdesk")));

        Map<String, String> mapping = buildNormalizedMapping(props);

        assertEquals("ROLE_MY_APP_ADMIN", mapping.get("GROUPB-ADMINS"));
        assertEquals("ROLE_MY_APP_ADMIN", mapping.get("GROUPB-HELPDESK"));
    }

    @Test
    void normalizedMappingEmptyRoleToAdGroups() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        // Default is already empty – mapping should be empty too
        Map<String, String> mapping = buildNormalizedMapping(props);
        assertTrue(mapping.isEmpty());
    }

    // ------------------------------------------------------------------
    // Authorities mapper – successful mapping
    // ------------------------------------------------------------------

    @Test
    void authoritiesMapperMapsKnownGroup() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of("MY_APP_USER", List.of("GroupA-Users")));

        Map<String, String> mapping = buildNormalizedMapping(props);
        // The DefaultLdapAuthoritiesPopulator prefixes group CNs with ROLE_ and uppercases
        List<GrantedAuthority> input = List.of(new SimpleGrantedAuthority("ROLE_GROUPA-USERS"));

        Set<String> roles = toRoleSet(mapAuthorities(input, mapping));

        assertTrue(roles.contains("ROLE_MY_APP_USER"));
    }

    @Test
    void authoritiesMapperMapsMultipleGroupsToSameRole() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of(
                "MY_APP_ADMIN", List.of("GroupB-Admins", "GroupB-Helpdesk")));

        Map<String, String> mapping = buildNormalizedMapping(props);
        List<GrantedAuthority> input = List.of(
                new SimpleGrantedAuthority("ROLE_GROUPB-HELPDESK"));

        Set<String> roles = toRoleSet(mapAuthorities(input, mapping));

        assertTrue(roles.contains("ROLE_MY_APP_ADMIN"),
                "GroupB-Helpdesk should map to MY_APP_ADMIN");
    }

    @Test
    void authoritiesMapperCanAssignMultipleRoles() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of(
                "MY_APP_USER", List.of("GroupA-Users"),
                "MY_APP_ADMIN", List.of("GroupB-Admins")));

        Map<String, String> mapping = buildNormalizedMapping(props);
        List<GrantedAuthority> input = List.of(
                new SimpleGrantedAuthority("ROLE_GROUPA-USERS"),
                new SimpleGrantedAuthority("ROLE_GROUPB-ADMINS"));

        Set<String> roles = toRoleSet(mapAuthorities(input, mapping));

        assertTrue(roles.contains("ROLE_MY_APP_USER"));
        assertTrue(roles.contains("ROLE_MY_APP_ADMIN"));
    }

    // ------------------------------------------------------------------
    // Fallback behavior
    // ------------------------------------------------------------------

    @Test
    void authoritiesMapperFallsBackWhenNoGroupsMatch() {
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        props.setRoleToAdGroups(Map.of("MY_APP_ADMIN", List.of("GroupB-Admins")));

        Map<String, String> mapping = buildNormalizedMapping(props);
        // User is only in GroupA-Users which is not mapped
        List<GrantedAuthority> input = List.of(new SimpleGrantedAuthority("ROLE_GROUPA-USERS"));

        Set<String> roles = toRoleSet(mapAuthorities(input, mapping));

        assertEquals(Set.of("ROLE_MY_APP_USER"), roles,
                "Should fall back to ROLE_MY_APP_USER when no groups match");
    }

    @Test
    void authoritiesMapperFallsBackOnEmptyAuthorities() {
        Map<String, String> mapping = Map.of();
        Set<String> roles = toRoleSet(mapAuthorities(List.of(), mapping));
        assertEquals(Set.of("ROLE_MY_APP_USER"), roles,
                "Empty authorities should yield the fallback role");
    }

    @Test
    void authoritiesMapperFallsBackOnNullAuthorities() {
        Map<String, String> mapping = Map.of();
        Set<String> roles = toRoleSet(mapAuthorities(null, mapping));
        assertEquals(Set.of("ROLE_MY_APP_USER"), roles,
                "Null authorities should yield the fallback role");
    }

    @Test
    void authoritiesMapperFallsBackWhenMappingIsEmpty() {
        // No roleToAdGroups configured → empty mapping → fallback role
        ActiveDirectoryProperties props = new ActiveDirectoryProperties();
        Map<String, String> mapping = buildNormalizedMapping(props);

        List<GrantedAuthority> input = List.of(new SimpleGrantedAuthority("ROLE_GROUPA-USERS"));
        Set<String> roles = toRoleSet(mapAuthorities(input, mapping));

        assertEquals(Set.of("ROLE_MY_APP_USER"), roles,
                "Empty mapping should yield the fallback role");
    }
}
