package com.demo.application.security.auth;

import com.demo.application.user.UserRepository;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.User;
import com.demo.shared.security.RoleInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Just-in-time provisioning for users who authenticate via Active Directory but have no
 * local record yet. Creates the user with the default self-service role so they get
 * tailored access to their own objects. Database-authenticated admins already exist and
 * are left untouched.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SelfServiceProvisioningService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provisionIfAbsent(String username) {
        if (username == null || username.isBlank() || userRepository.findByUsername(username).isPresent()) {
            return;
        }
        Role selfService = roleRepository.findByName(RoleInitializationService.USER).orElse(null);
        if (selfService == null) {
            log.warn("Self-service role '{}' not found; cannot provision user '{}'",
                    RoleInitializationService.USER, username);
            return;
        }
        User user = User.builder()
                .username(username)
                .email(username + "@ad.local")
                .build();
        user.getRoles().add(selfService);
        userRepository.save(user);
        log.info("Provisioned AD-authenticated user '{}' with the self-service role", username);
    }
}
