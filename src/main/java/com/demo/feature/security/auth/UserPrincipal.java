package com.demo.feature.security.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated caller as the application sees it: the {@code users} row
 * id and username, nothing else.
 *
 * <p>Implements {@link UserDetails} only so Spring Security can carry it as the
 * {@code Authentication} principal. It deliberately exposes no authorities —
 * permissions are resolved per request from role assignments, not from the
 * principal — and no password, because the application never holds one.
 */
public record UserPrincipal(Long id, String username) implements UserDetails {

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
