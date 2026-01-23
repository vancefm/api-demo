package com.demo.application.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.ldap")
public class LdapProperties {
    /**
     * Enable LDAP authentication.
     */
    private boolean enabled = true;

    /**
     * LDAP/Active Directory connection URLs.
     */
    private String urls = "ldap://ad.example.com:389";

    /**
     * Base DN for LDAP searches.
     */
    private String baseDn = "DC=example,DC=com";

    /**
     * User search filter (e.g., (sAMAccountName={0})).
     */
    private String userSearchFilter = "(sAMAccountName={0})";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }
}
