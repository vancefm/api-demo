package com.demo.application.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "security.active-directory")
public class ActiveDirectoryProperties {
    /**
     * Enable Active Directory authentication. Defaults to {@code false} so that AD is
     * only activated when explicitly opted in via {@code security.active-directory.enabled=true}.
     */
    private boolean enabled = false;

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

    /**
     * Mapping from AD group common names (CNs) to application role names.
     * Keys are the raw CN values of the AD groups (case-insensitive matching is applied
     * at runtime). Values are the Spring Security role names, with or without the
     * {@code ROLE_} prefix – the prefix is added automatically when absent.
     *
     * <p><strong>Note:</strong> The default values below are illustrative examples
     * based on the demo LDIF structure. Replace them with your actual AD group CNs
     * before deploying to a real Active Directory environment.</p>
     *
     * <p>Example {@code application.yml} snippet:</p>
     * <pre>
     * security:
     *   active-directory:
     *     group-role-mapping:
     *       My-AD-Group-Users: MY_APP_USER
     *       My-AD-Group-Admins: MY_APP_ADMIN
     *       My-AD-Group-SuperAdmins: MY_APP_SUPERADMIN
     * </pre>
     *
     * <p>When an authenticated user belongs to none of the mapped groups,
     * the fallback role {@code ROLE_MY_APP_USER} is assigned.</p>
     */
    private Map<String, String> groupRoleMapping = new LinkedHashMap<>(Map.of(
            "GroupA-Users", "ROLE_MY_APP_USER",
            "GroupB-Admins", "ROLE_MY_APP_ADMIN",
            "GroupC-SuperAdmins", "ROLE_MY_APP_SUPERADMIN"
    ));

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

    public Map<String, String> getGroupRoleMapping() {
        return groupRoleMapping;
    }

    public void setGroupRoleMapping(Map<String, String> groupRoleMapping) {
        this.groupRoleMapping = groupRoleMapping;
    }
}