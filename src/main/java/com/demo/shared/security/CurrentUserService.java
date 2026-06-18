package com.demo.shared.security;

import com.demo.application.security.auth.SelfServiceProvisioningService;
import com.demo.application.user.UserRepository;
import com.demo.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves the authenticated domain {@link User} from the Spring Security context,
 * uniformly across the DB ({@link UserPrincipal}), JWT and Active Directory paths.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final SelfServiceProvisioningService provisioningService;

    @Transactional(readOnly = true)
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = authenticatedName(auth);
        if (name == null) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.getUser());
        }
        return userRepository.findByUsername(name);
    }

    /**
     * Resolves the current user, just-in-time provisioning a self-service record for an
     * authenticated principal that has no local user yet (e.g. an AD/LDAP user on first access).
     */
    public User requireCurrentUser() {
        Optional<User> existing = getCurrentUser();
        if (existing.isPresent()) {
            return existing.get();
        }
        String name = authenticatedName(SecurityContextHolder.getContext().getAuthentication());
        if (name != null) {
            provisioningService.provisionIfAbsent(name);
            Optional<User> provisioned = userRepository.findByUsername(name);
            if (provisioned.isPresent()) {
                return provisioned.get();
            }
        }
        throw new AccessDeniedException("No authenticated user in context");
    }

    /** Returns the authenticated principal's username, or {@code null} if unauthenticated/anonymous. */
    private String authenticatedName(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }
}
