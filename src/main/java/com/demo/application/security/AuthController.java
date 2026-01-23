package com.demo.application.security;

import com.demo.application.security.jwt.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        // TODO: Authenticate against LDAP first, then DB fallback with BCrypt.
        // For now, issue a token placeholder if username provided.
        if (req == null || req.username == null || req.username.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String token = jwtService.createToken(req.username, Map.of("sub", req.username));
        Map<String, Object> resp = new HashMap<>();
        resp.put("access_token", token);
        resp.put("token_type", "Bearer");
        resp.put("expires_in", 7200);
        return ResponseEntity.ok(resp);
    }
}
