package com.demo.shared.security;

import com.demo.domain.user.User;

/**
 * Utility class for managing authentication context.
 * In a real application, this would integrate with Spring Security.
 * For this demo, we provide a simple ThreadLocal-based implementation.
 */
public class AuthenticationContext {
    
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    
    /**
     * Set the current authenticated user.
     */
    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }
    
    /**
     * Get the current authenticated user.
     */
    public static User getCurrentUser() {
        return currentUser.get();
    }
    
    /**
     * Clear the current authentication context.
     */
    public static void clear() {
        currentUser.remove();
    }
    
    /**
     * Check if a user is currently authenticated.
     */
    public static boolean isAuthenticated() {
        return currentUser.get() != null;
    }
}
