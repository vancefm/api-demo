package com.demo.shared.security;

import com.demo.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Security principal representing an authenticated user.
 */
@RequiredArgsConstructor
public class CustomUserPrincipal {
    
    @Getter
    private final User user;
    
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
