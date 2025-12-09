package com.demo.shared.security;

/**
 * Security principal representing an API token authentication.
 */
public class ApiTokenPrincipal {
    
    private final String token;
    private final String clientId;
    
    public ApiTokenPrincipal(String token, String clientId) {
        this.token = token;
        this.clientId = clientId;
    }
    
    public String getToken() {
        return token;
    }
    
    public String getClientId() {
        return clientId;
    }
}
