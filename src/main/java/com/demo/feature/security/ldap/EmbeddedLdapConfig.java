package com.demo.feature.security.ldap;

import com.demo.feature.security.auth.AppUserDetailsContextMapper;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;

import java.io.InputStream;

/**
 * Starts an in-process UnboundID LDAP server and wires the authentication
 * provider that binds against it.
 *
 * <p>The directory is the application's <em>only</em> credential store: the
 * {@code users} table never holds passwords, and the application never writes
 * to the directory. Callers authenticate with HTTP Basic; the provider binds as
 * {@code uid=<username>,ou=people} and, on success,
 * {@link AppUserDetailsContextMapper} resolves (or provisions) the matching
 * {@code users} row. LDAP groups are deliberately not read — authorization is
 * decided solely by the role assignments stored in the database.
 */
@Configuration
@EnableConfigurationProperties(LdapProperties.class)
@Slf4j
public class EmbeddedLdapConfig {

    private InMemoryDirectoryServer directoryServer;

    @Bean
    public InMemoryDirectoryServer embeddedLdapServer(LdapProperties properties, ResourceLoader resourceLoader)
            throws Exception {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(properties.getBaseDn());
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", properties.getPort()));
        // The LDIF is hand-written demo data; schema checking adds nothing but failure modes.
        config.setSchema(null);

        directoryServer = new InMemoryDirectoryServer(config);

        Resource ldif = resourceLoader.getResource(properties.getLdif());
        try (InputStream in = ldif.getInputStream()) {
            directoryServer.importFromLDIF(true, new LDIFReader(in));
        }

        directoryServer.startListening();
        log.info("Embedded LDAP server listening on port {} with base DN {} (loaded {})",
            directoryServer.getListenPort(), properties.getBaseDn(), properties.getLdif());
        return directoryServer;
    }

    @Bean
    public DefaultSpringSecurityContextSource ldapContextSource(InMemoryDirectoryServer server,
                                                                LdapProperties properties) {
        String url = "ldap://localhost:" + server.getListenPort() + "/" + properties.getBaseDn();
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    /**
     * Bind-authenticates the caller. {@link NullLdapAuthoritiesPopulator} keeps
     * LDAP group membership out of the picture; the context mapper supplies the
     * principal the rest of the application works with.
     */
    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(DefaultSpringSecurityContextSource contextSource,
                                                                 LdapProperties properties,
                                                                 AppUserDetailsContextMapper userDetailsMapper) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[] {properties.getUserDnPattern()});

        LdapAuthenticationProvider provider =
            new LdapAuthenticationProvider(authenticator, new NullLdapAuthoritiesPopulator());
        provider.setUserDetailsContextMapper(userDetailsMapper);
        return provider;
    }

    @PreDestroy
    public void stopServer() {
        if (directoryServer != null) {
            directoryServer.shutDown(true);
        }
    }
}
