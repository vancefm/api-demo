package com.demo.shared.security;

import com.demo.domain.user.User;

/**
 * Security principal representing an authenticated user.
 */
public class CustomUserPrincipal {
    
    private final User user;
    
    public CustomUserPrincipal(User user) {
        this.user = user;
    }
    
    public User getUser() {
        return user;
    }
    
    public String getUsername() {
        return user.getUsername();
    }
    
    public String getDepartment() {
        return user.getDepartment();
    }
    
    public String getRoleName() {
        return user.getRole().getName();
    }
}
