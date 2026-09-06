package com.demo.feature.security.auth;

import com.demo.feature.user.User;
import com.demo.feature.user.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * Turns a successful LDAP bind into the application's {@link UserPrincipal}.
 *
 * <p>The directory proves <em>who</em> the caller is; the {@code users} table is
 * where their profile and role assignments live. The two are linked by
 * username. A directory user with no {@code users} row yet is provisioned on
 * first login from the entry's attributes and starts with no role assignments —
 * they can authenticate but cannot do anything until someone grants them a role.
 */
@Component
@RequiredArgsConstructor
public class AppUserDetailsContextMapper implements UserDetailsContextMapper {

    private static final String FALLBACK_EMAIL_DOMAIN = "example.com";

    private final UserManagementService userManagementService;

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
        String uid = firstNonBlank(ctx.getStringAttribute("uid"), username);
        String email = firstNonBlank(ctx.getStringAttribute("mail"), uid + "@" + FALLBACK_EMAIL_DOMAIN);
        String firstName = ctx.getStringAttribute("givenName");
        String lastName = ctx.getStringAttribute("sn");

        User user = userManagementService.findOrProvision(uid, email, firstName, lastName);
        return new UserPrincipal(user.getId(), user.getUsername());
    }

    /**
     * The application never writes to the directory.
     */
    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("The application does not write to the directory");
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }
}
