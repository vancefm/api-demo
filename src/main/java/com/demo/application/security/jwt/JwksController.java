package com.demo.application.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the public JWKS endpoint used by clients/gateways to verify JWT signatures.
 */
@RestController
@RequestMapping("/.well-known")
public class JwksController {
    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Returns the public JWK set (JSON) for JWT signature verification.
     */
    @GetMapping("/jwks.json")
    public ResponseEntity<String> jwks() {
        JWKSet set = jwtService.getPublicJwkSet();
        // Return JSON representation (could be empty if no key configured)
        return ResponseEntity.ok(set.toJSONObject().toString());
    }
}
