package com.demo.application.security.filter;

import com.demo.application.security.jwt.JwtService;
import com.demo.application.security.token.ApiTokenRepository;
import com.demo.application.user.UserRepository;
import com.demo.shared.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.util.Collections;
import java.time.LocalDateTime;

/**
 * Validates Bearer JWTs and establishes authentication in the security context.
 * Falls back to persistent API tokens when JWT validation fails.
 *
 * <p>When the token subject (or API-token owner) resolves to a known domain user, the
 * authentication principal is a {@link UserPrincipal} carrying the user's role-derived
 * authorities and department designation, so method security and the authorization layer
 * have a single source of truth.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ApiTokenRepository apiTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtAuthenticationFilter(JwtService jwtService, ApiTokenRepository apiTokenRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.apiTokenRepository = apiTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.validateToken(token)) {
                authenticateJwt(request, token);
            } else {
                authenticateApiToken(request, token);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * JWT path: resolve the subject to a domain user when possible so authorities and
     * department context are available; otherwise authenticate with the bare subject.
     */
    private void authenticateJwt(HttpServletRequest request, String token) {
        String subject = token;
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (jwt.getJWTClaimsSet().getSubject() != null) {
                subject = jwt.getJWTClaimsSet().getSubject();
            }
        } catch (Exception _) {
            // fall back to the raw token as the principal name
        }

        UsernamePasswordAuthenticationToken auth = userRepository.findByUsername(subject)
                .map(user -> {
                    UserPrincipal principal = new UserPrincipal(user);
                    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                })
                .orElseGet(() -> new UsernamePasswordAuthenticationToken(token, null, Collections.emptyList()));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Fallback: persistent API token format (tokenId.secret). Verifies hash + expiry,
     * then authenticates as the owning user via a {@link UserPrincipal}.
     */
    private void authenticateApiToken(HttpServletRequest request, String token) {
        if (!token.contains(".")) {
            return;
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return;
        }
        String tokenId = parts[0];
        String secret = parts[1];
        apiTokenRepository.findByTokenId(tokenId).ifPresent(t -> {
            if (!t.isRevoked() && t.getExpiresAt() != null && t.getExpiresAt().isAfter(LocalDateTime.now())
                    && passwordEncoder.matches(secret, t.getTokenHash())) {
                userRepository.findById(t.getOwnerUserId()).ifPresent(user -> {
                    UserPrincipal principal = new UserPrincipal(user);
                    UsernamePasswordAuthenticationToken a =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    a.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(a);
                });
            }
        });
    }
}
