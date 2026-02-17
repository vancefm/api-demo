package com.demo.application.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.active-directory")
@Getter
@Setter
public class ActiveDirectoryProperties {
    /**
     * Enable Active Directory authentication.
     */
    private boolean enabled = true;

    /**
     * Active Directory connection URL.
     */
    private String url = "ldap://ad.example.com:389";

    /**
     * Active Directory domain (e.g., example.com).
     */
    private String domain = "example.com";

    /**
     * Root DN for AD searches.
     */
    private String rootDn = "DC=example,DC=com";

    /**
     * User search filter (sAMAccountName by default).
     */
    private String userSearchFilter = "(sAMAccountName={0})";

    /**
     * Group search base (optional).
     */
    private String groupSearchBase = "";

    /**
     * Group search filter (defaults to AD member lookup).
     */
    private String groupSearchFilter = "(member={0})";

    /**
     * Manager DN for group searches (optional).
     */
    private String managerDn = "";

    /**
     * Manager password for group searches (optional).
     */
    private String managerPassword = "";
}