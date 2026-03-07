package com.demo.application.security.auth;

import com.demo.application.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtService jwtService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        if (req == null || req.username == null || req.username.isBlank() || req.password == null) {
            return ResponseEntity.badRequest().build();
        }

        Authentication auth;
        try {
            Authentication authReq = new UsernamePasswordAuthenticationToken(req.username, req.password);
            auth = authenticationManager.authenticate(authReq);
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }

        // Extract roles from granted authorities and include in JWT claims
        java.util.List<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();

        String token = jwtService.createToken(req.username, Map.of("roles", roles));
        Map<String, Object> resp = new HashMap<>();
        resp.put("access_token", token);
        resp.put("token_type", "Bearer");
        resp.put("expires_in", 7200);
        return ResponseEntity.ok(resp);
    }
}
