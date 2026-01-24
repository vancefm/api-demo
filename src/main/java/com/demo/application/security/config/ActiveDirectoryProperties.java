package com.demo.application.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.active-directory")
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRootDn() {
        return rootDn;
    }

    public void setRootDn(String rootDn) {
        this.rootDn = rootDn;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }

    public String getManagerDn() {
        return managerDn;
    }

    public void setManagerDn(String managerDn) {
        this.managerDn = managerDn;
    }

    public String getManagerPassword() {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }
}