package com.demo.application.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.Collections;

@Configuration
@EnableConfigurationProperties(LdapProperties.class)
@ConditionalOnProperty(prefix = "security.ldap", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LdapConfig {
    private final LdapProperties properties;

    public LdapConfig(LdapProperties properties) {
        this.properties = properties;
    }

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource ctx = new DefaultSpringSecurityContextSource(Collections.singletonList(properties.getUrls()), properties.getBaseDn());
        // In real deployment you may need managerDn/managerPassword for search; configure via env variables.
        return ctx;
    }

    @Bean
    public BindAuthenticator bindAuthenticator(LdapContextSource contextSource) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(new FilterBasedLdapUserSearch("", properties.getUserSearchFilter(), contextSource));
        return authenticator;
    }

    @Bean
    public DefaultLdapAuthoritiesPopulator authoritiesPopulator(LdapContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator pop = new DefaultLdapAuthoritiesPopulator(contextSource, null);
        pop.setIgnorePartialResultException(true);
        return pop;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(BindAuthenticator bindAuthenticator, DefaultLdapAuthoritiesPopulator populator) {
        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(bindAuthenticator, populator);
        return provider;
    }
}
