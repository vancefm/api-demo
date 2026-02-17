package com.demo.shared.config;

import com.demo.application.user.UserRepository;
import com.demo.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration that enables automatic population of
 * {@code @CreatedBy}, {@code @LastModifiedBy}, {@code @CreatedDate},
 * and {@code @LastModifiedDate} annotated fields on entities.
 *
 * <p>The {@link AuditorAware} bean resolves the current auditor by
 * extracting the username from the Spring Security context and
 * looking up the corresponding {@link User} entity.</p>
 */
@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class JpaAuditingConfig {

    private final UserRepository userRepository;

    @Bean
    public AuditorAware<User> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }
            String username = authentication.getName();
            return userRepository.findByUsername(username);
        };
    }
}
