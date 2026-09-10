package com.demo.feature.security.ldap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the embedded LDAP server that authenticates API callers.
 *
 * <p>Bound from {@code app.ldap.*} in {@code application.yml}.
 */
@ConfigurationProperties(prefix = "app.ldap")
@Getter
@Setter
public class LdapProperties {

    /**
     * Root of the directory tree; every entry in the LDIF must live under it.
     */
    private String baseDn = "dc=demo,dc=com";

    /**
     * Spring resource location of the LDIF loaded into the server at startup.
     */
    private String ldif = "classpath:ldap-users.ldif";

    /**
     * TCP port the embedded server listens on. {@code 0} picks a free port,
     * which is what tests want; pin a port to connect an external LDAP browser.
     */
    private int port = 0;

    /**
     * Relative DN pattern used to bind as the caller; {@code {0}} is the username.
     */
    private String userDnPattern = "uid={0},ou=people";
}
