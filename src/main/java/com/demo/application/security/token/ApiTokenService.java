package com.demo.application.security.token;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiTokenService {
    private final ApiTokenRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public ApiTokenService(ApiTokenRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new persistent API token for ownerUserId.
     * Returns the stored ApiToken where tokenHash temporarily contains the raw token value
     * so callers can return it once. In the DB tokenHash stores the hashed secret.
     */
    public CreateTokenResponse createToken(Long ownerUserId, String scopes, Integer expiryDays) {
        String tokenId = UUID.randomUUID().toString();
        byte[] secretBytes = new byte[48]; // 384 bits
        random.nextBytes(secretBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        String tokenValue = tokenId + "." + secret;

        String hashed = passwordEncoder.encode(secret);

        ApiToken t = new ApiToken();
        t.setTokenId(tokenId);
        t.setTokenHash(hashed);
        t.setOwnerUserId(ownerUserId);
        t.setScopes(scopes);
        t.setCreatedAt(LocalDateTime.now());
        if (expiryDays == null) {
            expiryDays = 365;
        }
        t.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        t.setRevoked(false);
        repo.save(t);

        return new CreateTokenResponse(tokenId, tokenValue);
    }

    public Optional<ApiToken> findByTokenId(String tokenId) {
        return repo.findByTokenId(tokenId);
    }
}
