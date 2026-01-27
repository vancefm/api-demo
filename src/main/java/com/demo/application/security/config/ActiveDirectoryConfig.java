package com.demo.application.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableConfigurationProperties(ActiveDirectoryProperties.class)
@ConditionalOnProperty(prefix = "security.active-directory", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ActiveDirectoryConfig {
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
     * Builds the Active Directory authentication provider and applies role fallback.
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
        provider.setAuthoritiesMapper(authorities -> {
            if (authorities == null || authorities.isEmpty()) {
                // AD-authenticated users with no groups receive the MY_APP_USER role.
                return List.of(new SimpleGrantedAuthority("ROLE_MY_APP_USER"));
            }
            return new ArrayList<>(authorities);
        });
        // Translate AD sub-error codes into Spring Security exceptions.
        provider.setConvertSubErrorCodesToExceptions(true);
        return provider;
    }
}