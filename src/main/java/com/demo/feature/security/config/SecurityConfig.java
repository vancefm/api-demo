package com.demo.feature.security.config;

import com.demo.feature.security.auth.ProblemDetailAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security: who may reach the API at all.
 *
 * <p>Every {@code /api/**} request must carry HTTP Basic credentials that bind
 * successfully against the embedded LDAP server (see
 * {@code feature.security.ldap.EmbeddedLdapConfig}). Nothing else is decided
 * here — <em>what</em> an authenticated caller may do is enforced per operation
 * in the service layer by the RBAC feature, not by URL patterns.
 *
 * <p>Operational and documentation endpoints (actuator, Swagger UI, OpenAPI
 * document, H2 console) stay open. The API is stateless: no session, no CSRF.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LdapAuthenticationProvider ldapAuthenticationProvider,
                                                   ProblemDetailAuthenticationEntryPoint entryPoint) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // The H2 console renders in frames; the default DENY blanks it.
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authenticationProvider(ldapAuthenticationProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
            .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
            .anonymous(Customizer.withDefaults());
        return http.build();
    }
}
