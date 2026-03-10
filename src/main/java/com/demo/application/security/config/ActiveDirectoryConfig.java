package com.demo.application.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(ActiveDirectoryProperties.class)
@ConditionalOnProperty(prefix = "security.active-directory", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ActiveDirectoryConfig {
    private static final String ROLE_PREFIX = "ROLE_";

    private final ActiveDirectoryProperties properties;

    public ActiveDirectoryConfig(ActiveDirectoryProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates the AD LDAP context used for binding and directory searches.
     */
    @Bean
    public LdapContextSource activeDirectoryContextSource() {
        // Build the LDAP context for Active Directory connectivity.
        LdapContextSource contextSource = new DefaultSpringSecurityContextSource(
                Collections.singletonList(properties.getUrl()),
                properties.getRootDn()
        );
        if (StringUtils.hasText(properties.getManagerDn())) {
            // Optional service account for group/user searches.
            contextSource.setUserDn(properties.getManagerDn());
            contextSource.setPassword(properties.getManagerPassword());
        }
        return contextSource;
    }

    /**
     * Configures how AD groups are discovered and mapped to authorities.
     */
    @Bean
    public DefaultLdapAuthoritiesPopulator activeDirectoryAuthoritiesPopulator(LdapContextSource contextSource) {
        // Use a specific group search base when provided, otherwise fall back to the root DN.
        String groupSearchBase = StringUtils.hasText(properties.getGroupSearchBase())
                ? properties.getGroupSearchBase()
                : null;
        DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
        if (StringUtils.hasText(properties.getGroupSearchFilter())) {
            // Configure the group membership lookup filter (member by default).
            populator.setGroupSearchFilter(properties.getGroupSearchFilter());
        }
        // AD can return partial results; ignore those to avoid auth failures.
        populator.setIgnorePartialResultException(true);
        return populator;
    }

    /**
     * Builds the Active Directory authentication provider and applies explicit role mapping.
     *
     * <p>Authority resolution order:</p>
     * <ol>
     *   <li>AD groups are loaded by the {@link DefaultLdapAuthoritiesPopulator}.</li>
     *   <li>Each group CN is matched (case-insensitively) against the configured
     *       {@link ActiveDirectoryProperties#getRoleToAdGroups()} mapping (inverted at
     *       startup into a groupCN → role lookup).</li>
     *   <li>Matched groups are converted to application roles, ensuring the {@code ROLE_}
     *       prefix is present.</li>
     *   <li>When no groups match, the fallback role {@code ROLE_MY_APP_USER} is assigned.</li>
     * </ol>
     */
    @Bean
    public ActiveDirectoryLdapAuthenticationProvider activeDirectoryAuthenticationProvider(
            DefaultLdapAuthoritiesPopulator populator) {
        // AD auth provider handles bind and user validation against the directory.
        ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(
                properties.getDomain(),
                properties.getUrl(),
                properties.getRootDn()
        );
        // Use sAMAccountName-based lookup by default.
        provider.setSearchFilter(properties.getUserSearchFilter());
        // Map AD groups into Spring Security authorities.
        provider.setAuthoritiesPopulator(populator);

        // Invert roleToAdGroups (role -> [groupCN, ...]) into a normalised
        // groupCN (uppercase) -> ROLE_... lookup built once at startup.
        Map<String, String> normalizedGroupRoleMapping = new HashMap<>();
        if (properties.getRoleToAdGroups() != null) {
            for (Map.Entry<String, List<String>> entry : properties.getRoleToAdGroups().entrySet()) {
                String roleValue = entry.getKey();
                if (!roleValue.startsWith(ROLE_PREFIX)) {
                    roleValue = ROLE_PREFIX + roleValue;
                }
                List<String> groups = entry.getValue();
                if (groups == null) {
                    continue;
                }
                for (String groupCn : groups) {
                    if (!StringUtils.hasText(groupCn)) {
                        continue;
                    }
                    normalizedGroupRoleMapping.put(groupCn.toUpperCase(), roleValue);
                }
            }
        }
        final Map<String, String> groupRoleMapping = Collections.unmodifiableMap(normalizedGroupRoleMapping);

        provider.setAuthoritiesMapper(authorities -> {
            if (authorities == null || authorities.isEmpty()) {
                // AD-authenticated users with no groups receive the fallback role.
                return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + "MY_APP_USER"));
            }

            Set<GrantedAuthority> mapped = new HashSet<>();
            for (GrantedAuthority authority : authorities) {
                String name = authority.getAuthority().toUpperCase();
                // Strip ROLE_ prefix added by the authorities populator before lookup.
                if (name.startsWith(ROLE_PREFIX)) {
                    name = name.substring(ROLE_PREFIX.length());
                }
                String mappedRole = groupRoleMapping.get(name);
                if (mappedRole != null) {
                    mapped.add(new SimpleGrantedAuthority(mappedRole));
                }
            }

            if (mapped.isEmpty()) {
                // AD user authenticated but no recognised groups – assign fallback role.
                mapped.add(new SimpleGrantedAuthority(ROLE_PREFIX + "MY_APP_USER"));
            }
            return new ArrayList<>(mapped);
        });
        // Translate AD sub-error codes into Spring Security exceptions.
        provider.setConvertSubErrorCodesToExceptions(true);
        return provider;
    }
}