package com.demo.shared.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Security principal representing an API token authentication.
 */
@Getter
@RequiredArgsConstructor
public class ApiTokenPrincipal {
    
    private final String token;
    private final String clientId;
}
