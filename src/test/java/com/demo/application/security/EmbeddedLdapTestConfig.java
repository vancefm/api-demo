package com.demo.application.security;

import com.demo.application.security.db.DbUserDetailsService;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test configuration that starts an embedded UnboundID LDAP server,
 * loads test users/groups from an LDIF file, and wires an
 * {@link LdapAuthenticationProvider} that maps LDAP groups to application roles.
 *
 * <p>Group → Role mapping:</p>
 * <ul>
 *   <li>GroupA-Users → ROLE_MY_APP_USER</li>
 *   <li>GroupB-Admins → ROLE_MY_APP_ADMIN</li>
 *   <li>GroupC-SuperAdmins → ROLE_MY_APP_SUPERADMIN</li>
 * </ul>
 */
@TestConfiguration
public class EmbeddedLdapTestConfig {

    private InMemoryDirectoryServer directoryServer;

    @Bean
    public InMemoryDirectoryServer embeddedLdapServer() throws Exception {
        InMemoryDirectoryServerConfig config =
                new InMemoryDirectoryServerConfig("dc=demo,dc=com");
        config.setListenerConfigs(
                InMemoryListenerConfig.createLDAPConfig("default", 0));
        config.setSchema(null); // disable schema validation for test simplicity

        directoryServer = new InMemoryDirectoryServer(config);

        try (InputStream is = new ClassPathResource("test-ldap-users.ldif").getInputStream()) {
            directoryServer.importFromLDIF(true, new LDIFReader(is));
        }

        directoryServer.startListening();
        return directoryServer;
    }

    @Bean
    public DefaultSpringSecurityContextSource embeddedLdapContextSource(
            InMemoryDirectoryServer server) {
        String url = "ldap://localhost:" + server.getListenPort() + "/dc=demo,dc=com";
        DefaultSpringSecurityContextSource contextSource =
                new DefaultSpringSecurityContextSource(url);
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(
            DefaultSpringSecurityContextSource contextSource) {

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[]{"uid={0},ou=people"});

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, "ou=groups");
        authoritiesPopulator.setGroupSearchFilter("(member={0})");
        authoritiesPopulator.setSearchSubtree(true);

        LdapAuthenticationProvider provider =
                new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
        provider.setAuthoritiesMapper(this::mapAuthorities);
        return provider;
    }

    /**
     * Provides the primary {@link AuthenticationManager} for integration tests.
     *
     * <p>The embedded LDAP provider is tried first, with the DB/DAO provider as
     * fallback. This bean takes precedence over the production {@code SecurityConfig}
     * authentication manager so that tests authenticate against the in-memory LDAP
     * server rather than a real Active Directory instance.</p>
     */
    @Bean
    @Primary
    public AuthenticationManager embeddedLdapAuthenticationManager(
            LdapAuthenticationProvider ldapAuthenticationProvider,
            PasswordEncoder passwordEncoder,
            DbUserDetailsService dbUserDetailsService) {
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(dbUserDetailsService);
        daoProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(ldapAuthenticationProvider, daoProvider);
    }

    /**
     * Translates LDAP group authorities to application role authorities.
     * The {@link DefaultLdapAuthoritiesPopulator} produces authorities like
     * {@code ROLE_GROUPA-USERS} (uppercase CN prefixed with ROLE_).
     */
    private Collection<? extends GrantedAuthority> mapAuthorities(
            Collection<? extends GrantedAuthority> authorities) {

        Map<String, String> groupToRole = Map.of(
                "ROLE_GROUPA-USERS", "ROLE_MY_APP_USER",
                "ROLE_GROUPB-ADMINS", "ROLE_MY_APP_ADMIN",
                "ROLE_GROUPC-SUPERADMINS", "ROLE_MY_APP_SUPERADMIN"
        );

        Set<GrantedAuthority> mapped = new HashSet<>();
        for (GrantedAuthority authority : authorities) {
            String role = groupToRole.get(authority.getAuthority().toUpperCase());
            if (role != null) {
                mapped.add(new SimpleGrantedAuthority(role));
            }
        }

        // Fallback: users with no recognised groups receive the default user role
        if (mapped.isEmpty()) {
            mapped.add(new SimpleGrantedAuthority("ROLE_MY_APP_USER"));
        }

        return mapped;
    }

    @PreDestroy
    public void stopServer() {
        if (directoryServer != null) {
            directoryServer.shutDown(true);
        }
    }
}
