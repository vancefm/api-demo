package com.demo.application.security;

import com.demo.application.security.token.ApiToken;
import com.demo.application.security.token.ApiTokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tokens")
public class TokenController {
    private final ApiTokenRepository apiTokenRepository;

    public TokenController(ApiTokenRepository apiTokenRepository) {
        this.apiTokenRepository = apiTokenRepository;
    }

    @PostMapping
    public ResponseEntity<ApiToken> createToken(@RequestParam Long ownerUserId, @RequestParam(required = false) String scopes) {
        ApiToken token = new ApiToken();
        token.setTokenId(UUID.randomUUID().toString());
        token.setTokenHash("TODO:store-hash");
        token.setOwnerUserId(ownerUserId);
        token.setScopes(scopes);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(365));
        apiTokenRepository.save(token);
        return ResponseEntity.ok(token);
    }

    @GetMapping
    public ResponseEntity<List<ApiToken>> listTokens(@RequestParam Long ownerUserId) {
        List<ApiToken> all = apiTokenRepository.findAll();
        return ResponseEntity.ok(all);
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> revoke(@PathVariable String tokenId) {
        apiTokenRepository.findByTokenId(tokenId).ifPresent(t -> {
            t.setRevoked(true);
            apiTokenRepository.save(t);
        });
        return ResponseEntity.noContent().build();
    }
}
