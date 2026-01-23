package com.demo.application.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtProperties props;
    private RSAKey rsaJwk; // public/private JWK

    public JwtService(JwtProperties props) {
        this.props = props;
        // Load key material. If props.privateKey is blank, service will throw when creating tokens.
        // In a real deployment load from keystore or environment secret manager.
        if (props.getPrivateKey() != null && !props.getPrivateKey().isBlank()) {
            // TODO: parse PEM or JKS and construct RSAKey. For now, we do not construct from config.
        }
    }

    public String createToken(String subject, Map<String, Object> extraClaims) {
        if (props.getPrivateKey() == null || props.getPrivateKey().isBlank()) {
            throw new IllegalStateException("Private key not configured. Provide a RSA-4096 private key via keystore or environment variable as documented.");
        }

        try {
            // Placeholder implementation: in production, load the RSA private key and sign.
            // Here we return a UUID token string for local development.
            // TODO: Replace with a real SignedJWT using Nimbus and RSA-4096.
            return UUID.randomUUID().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean validateToken(String token) {
        // TODO: Implement JWT parsing and signature verification using the public JWK set
        // For now, return true for non-empty tokens to allow basic integration.
        return token != null && !token.isBlank();
    }

    public JWKSet getPublicJwkSet() {
        // If rsaJwk is available return public JWK set; otherwise return empty set.
        if (rsaJwk != null) {
            return new JWKSet(rsaJwk.toPublicJWK());
        }
        return new JWKSet();
    }
}
