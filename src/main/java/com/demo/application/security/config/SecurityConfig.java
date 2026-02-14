package com.demo.application.security.config;

import com.demo.application.security.db.DbUserDetailsService;
import com.demo.application.security.filter.JwtAuthenticationFilter;
import com.demo.application.security.jwt.JwtService;
import com.demo.application.security.token.ApiTokenRepository;
import com.demo.application.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final DbUserDetailsService dbUserDetailsService;
    private final ObjectProvider<ActiveDirectoryLdapAuthenticationProvider> activeDirectoryAuthenticationProvider;
    private final ObjectProvider<LdapAuthenticationProvider> ldapAuthenticationProvider;

    public SecurityConfig(JwtService jwtService,
                          DbUserDetailsService dbUserDetailsService,
                          ObjectProvider<ActiveDirectoryLdapAuthenticationProvider> activeDirectoryAuthenticationProvider,
                          ObjectProvider<LdapAuthenticationProvider> ldapAuthenticationProvider) {
        this.jwtService = jwtService;
        this.dbUserDetailsService = dbUserDetailsService;
        this.activeDirectoryAuthenticationProvider = activeDirectoryAuthenticationProvider;
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength is configured via application.yml (BCrypt rounds)
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PasswordEncoder passwordEncoder,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints: auth + JWKS for JWT verification + docs/health
                .requestMatchers("/api/v1/auth/**", "/.well-known/jwks.json", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/computer-systems").hasRole("MY_APP_USER")
                .requestMatchers("/api/v1/admin/**").hasRole("MY_APP_SUPERADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        // JWT filter: validates incoming Bearer tokens and sets SecurityContext
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    /**
     * Builds the JWT authentication filter that validates Bearer tokens and
     * establishes the Spring Security context for authenticated requests.
     */
    public JwtAuthenticationFilter jwtAuthenticationFilter(PasswordEncoder passwordEncoder,
                                                           ApiTokenRepository apiTokenRepository,
                                                           UserRepository userRepository) {
        return new JwtAuthenticationFilter(jwtService, apiTokenRepository, userRepository, passwordEncoder);
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(dbUserDetailsService);
        daoProvider.setPasswordEncoder(passwordEncoder);

        // Provider order: LDAP first (if available), then Active Directory (if enabled), then DB fallback
        List<AuthenticationProvider> providers = new ArrayList<>();

        LdapAuthenticationProvider ldapProvider = ldapAuthenticationProvider.getIfAvailable();
        if (ldapProvider != null) {
            providers.add(ldapProvider);
        }

        ActiveDirectoryLdapAuthenticationProvider adProvider = activeDirectoryAuthenticationProvider.getIfAvailable();
        if (adProvider != null) {
            providers.add(adProvider);
        }

        providers.add(daoProvider);
        return new ProviderManager(providers);
    }
}
