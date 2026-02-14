package com.demo.application.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying that the embedded LDAP server authenticates users
 * and maps LDAP group membership to the correct application roles.
 */
@SpringBootTest
@Import(EmbeddedLdapTestConfig.class)
class EmbeddedLdapAuthenticationIT {

    @Autowired
    private AuthenticationManager authenticationManager;

    // ------------------------------------------------------------------
    // GroupA-Users members (user role only)
    // ------------------------------------------------------------------

    @Test
    void user1AuthenticatesWithUserRole() {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("user1", "password1"));

        assertTrue(auth.isAuthenticated());
        Set<String> roles = rolesToSet(auth);
        assertTrue(roles.contains("ROLE_MY_APP_USER"),
                "user1 should have ROLE_MY_APP_USER");
        assertFalse(roles.contains("ROLE_MY_APP_ADMIN"),
                "user1 should not have ROLE_MY_APP_ADMIN");
        assertFalse(roles.contains("ROLE_MY_APP_SUPERADMIN"),
                "user1 should not have ROLE_MY_APP_SUPERADMIN");
    }

    @Test
    void user2AuthenticatesWithUserRole() {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("user2", "password2"));

        assertTrue(auth.isAuthenticated());
        Set<String> roles = rolesToSet(auth);
        assertTrue(roles.contains("ROLE_MY_APP_USER"),
                "user2 should have ROLE_MY_APP_USER");
        assertFalse(roles.contains("ROLE_MY_APP_ADMIN"),
                "user2 should not have ROLE_MY_APP_ADMIN");
    }

    // ------------------------------------------------------------------
    // GroupA-Users + GroupB-Admins member (dual roles)
    // ------------------------------------------------------------------

    @Test
    void admin1AuthenticatesWithUserAndAdminRoles() {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("admin1", "admin123"));

        assertTrue(auth.isAuthenticated());
        Set<String> roles = rolesToSet(auth);
        assertTrue(roles.contains("ROLE_MY_APP_USER"),
                "admin1 should have ROLE_MY_APP_USER (member of GroupA-Users)");
        assertTrue(roles.contains("ROLE_MY_APP_ADMIN"),
                "admin1 should have ROLE_MY_APP_ADMIN (member of GroupB-Admins)");
        assertFalse(roles.contains("ROLE_MY_APP_SUPERADMIN"),
                "admin1 should not have ROLE_MY_APP_SUPERADMIN");
    }

    // ------------------------------------------------------------------
    // GroupC-SuperAdmins member
    // ------------------------------------------------------------------

    @Test
    void superadmin1AuthenticatesWithSuperAdminRole() {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("superadmin1", "super123"));

        assertTrue(auth.isAuthenticated());
        Set<String> roles = rolesToSet(auth);
        assertTrue(roles.contains("ROLE_MY_APP_SUPERADMIN"),
                "superadmin1 should have ROLE_MY_APP_SUPERADMIN");
    }

    // ------------------------------------------------------------------
    // Negative cases
    // ------------------------------------------------------------------

    @Test
    void invalidCredentialsThrowsBadCredentials() {
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken("user1", "wrongpassword")));
    }

    @Test
    void unknownUserThrowsBadCredentials() {
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken("nonexistent", "password")));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Set<String> rolesToSet(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
