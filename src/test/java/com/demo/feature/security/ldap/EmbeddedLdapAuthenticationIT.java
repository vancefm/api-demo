package com.demo.feature.security.ldap;

import com.demo.feature.security.auth.UserPrincipal;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The embedded directory itself: it starts, holds the LDIF entries, and the
 * bind-based provider authenticates against it and hands back the application
 * principal (not a Spring Security default user).
 */
@SpringBootTest
@Transactional
class EmbeddedLdapAuthenticationIT {

    @Autowired
    private InMemoryDirectoryServer server;

    @Autowired
    private LdapAuthenticationProvider provider;

    @Test
    void serverIsListeningAndLoadedTheLdif() throws Exception {
        assertTrue(server.getListenPort() > 0);
        assertNotNull(server.getEntry("uid=admin,ou=people,dc=demo,dc=com"));
        assertNotNull(server.getEntry("uid=user1,ou=people,dc=demo,dc=com"));
    }

    @Test
    void validBindYieldsApplicationPrincipalWithNoAuthorities() {
        Authentication result = provider.authenticate(
            new UsernamePasswordAuthenticationToken("user1", "password1"));

        assertTrue(result.isAuthenticated());
        UserPrincipal principal = assertInstanceOf(UserPrincipal.class, result.getPrincipal());
        assertEquals("user1", principal.username());
        assertNotNull(principal.id(), "principal must be linked to a users row");
        // Spring Security 7 tags password logins with FACTOR_PASSWORD for its
        // multi-factor support; that is framework metadata, not a directory group.
        List<String> roleAuthorities = result.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> !authority.startsWith("FACTOR_"))
            .toList();
        assertTrue(roleAuthorities.isEmpty(),
            "LDAP groups must not become authorities, got " + roleAuthorities);
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
            new UsernamePasswordAuthenticationToken("user1", "wrong")));
    }

    @Test
    void unknownUserIsRejected() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
            new UsernamePasswordAuthenticationToken("ghost", "password")));
    }
}
