package com.demo.feature.security.auth;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Access to the caller of the current request.
 *
 * <p>This is the only seam between the authentication mechanism and the rest of
 * the application: services and the access-control layer ask this bean who is
 * calling and never touch {@link SecurityContextHolder} themselves. Replacing
 * the embedded LDAP login with another mechanism means producing a
 * {@link UserPrincipal} some other way; nothing downstream changes.
 */
@Component
public class CurrentUser {

    /**
     * The authenticated caller, or empty when the request is anonymous.
     */
    public Optional<UserPrincipal> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * The authenticated caller; throws when there is none. Every {@code /api/**}
     * route requires authentication at the filter level, so this only fires if
     * a service is invoked outside a request.
     */
    public UserPrincipal require() {
        return get().orElseThrow(() ->
            new AuthenticationCredentialsNotFoundException("No authenticated user in the current context"));
    }
}
