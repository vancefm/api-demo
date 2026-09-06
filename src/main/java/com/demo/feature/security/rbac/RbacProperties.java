package com.demo.feature.security.rbac;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RBAC settings, bound from {@code app.rbac.*}.
 */
@Component
@ConfigurationProperties(prefix = "app.rbac")
@Getter
@Setter
public class RbacProperties {

    private Bootstrap bootstrap = new Bootstrap();

    /**
     * What {@link RbacBootstrap} seeds at startup so the system is usable.
     */
    @Getter
    @Setter
    public static class Bootstrap {

        /**
         * Seed the SuperAdmin role and the bootstrap user's global grant.
         */
        private boolean enabled = true;

        /**
         * Directory username of the first administrator. Must exist in the LDIF
         * or nobody can log in with full rights.
         */
        private String username = "admin";

        /**
         * Email recorded on the bootstrap user's row (the directory's {@code mail}
         * is not consulted here because the row is created before any login).
         */
        private String email = "admin@example.com";

        /**
         * Name of the system role that holds every permission.
         */
        private String superAdminRole = "SuperAdmin";
    }
}
