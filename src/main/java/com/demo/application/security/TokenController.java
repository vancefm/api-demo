package com.demo.application.security;

import com.demo.application.security.token.ApiToken;
import com.demo.application.security.token.ApiTokenRepository;
import com.demo.application.security.token.ApiTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tokens")
public class TokenController {
    private final ApiTokenRepository apiTokenRepository;
    private final ApiTokenService apiTokenService;

    public TokenController(ApiTokenRepository apiTokenRepository, ApiTokenService apiTokenService) {
        this.apiTokenRepository = apiTokenRepository;
        this.apiTokenService = apiTokenService;
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createToken(@RequestParam Long ownerUserId, @RequestParam(required = false) String scopes) {
        var resp = apiTokenService.createToken(ownerUserId, scopes, null);
        return ResponseEntity.ok(Map.of(
                "token_id", resp.getTokenId(),
                "token_value", resp.getTokenValue()
        ));
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ApiToken>> listTokens(@RequestParam Long ownerUserId) {
        List<ApiToken> all = apiTokenRepository.findAll();
        // Do not expose tokenHash in API responses in production. This listing is for admin use.
        all.forEach(t -> t.setTokenHash(null));
        return ResponseEntity.ok(all);
    }

    @DeleteMapping("/{tokenId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> revoke(@PathVariable String tokenId) {
        apiTokenRepository.findByTokenId(tokenId).ifPresent(t -> {
            t.setRevoked(true);
            apiTokenRepository.save(t);
        });
        return ResponseEntity.noContent().build();
    }
}
