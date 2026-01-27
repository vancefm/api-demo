package com.demo.application.security.filter;

import com.demo.application.security.jwt.JwtService;
import com.demo.application.security.token.ApiTokenRepository;
import com.demo.application.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
            // JWT path: use subject claim as principal when available
                String principal = token;
                try {
                    SignedJWT jwt = SignedJWT.parse(token);
                    if (jwt.getJWTClaimsSet().getSubject() != null) {
                        principal = jwt.getJWTClaimsSet().getSubject();
                    }
                } catch (Exception ignored) { }

                // JWTs do not embed authorities in this filter; downstream services use roles claim if needed
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            else {
                // Fallback: persistent API token format (tokenId.secret)
                if (token.contains(".")) {
                    String[] parts = token.split("\\.", 2);
                    if (parts.length == 2) {
                        String tokenId = parts[0];
                        String secret = parts[1];
                        apiTokenRepository.findByTokenId(tokenId).ifPresent(t -> {
                            if (!t.isRevoked() && t.getExpiresAt() != null && t.getExpiresAt().isAfter(LocalDateTime.now())) {
                                if (passwordEncoder.matches(secret, t.getTokenHash())) {
                                    // Load owner user and set authentication
                                    userRepository.findById(t.getOwnerUserId()).ifPresent(user -> {
                                        SimpleGrantedAuthority auth = new SimpleGrantedAuthority("ROLE_" + user.getRole().getName());
                                        UsernamePasswordAuthenticationToken a = new UsernamePasswordAuthenticationToken(user.getUsername(), null, java.util.List.of(auth));
                                        a.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                        SecurityContextHolder.getContext().setAuthentication(a);
                                    });
                                }
                            }
                        });
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
