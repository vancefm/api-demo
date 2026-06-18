package com.demo.shared.security;

import com.demo.domain.user.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Supplies the current {@link User} for {@code @CreatedBy} auditing.
 *
 * <p>Resolves the user directly from the {@link UserPrincipal} in the security context and
 * never issues a query. This is deliberate: the auditor runs inside Hibernate flush
 * callbacks, so any repository query here would trigger a recursive auto-flush. When the
 * principal is not a {@code UserPrincipal} (e.g. a freshly provisioned AD/LDAP request or a
 * test mock user) auditing is simply skipped, leaving createdBy null.</p>
 */
@Component("auditorProvider")
public class CurrentUserAuditorAware implements AuditorAware<User> {

    @Override
    public Optional<User> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.getUser());
        }
        return Optional.empty();
    }
}
