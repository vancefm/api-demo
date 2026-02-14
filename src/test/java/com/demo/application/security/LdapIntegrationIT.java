package com.demo.application.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that exercise the full authentication and authorisation
 * flow using LDAP credentials against the embedded UnboundID LDAP server.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Login endpoint returns a JWT for valid LDAP credentials</li>
 *   <li>Invalid credentials are rejected with 401</li>
 *   <li>Authenticated users can access protected endpoints</li>
 *   <li>Regular users cannot access admin-only endpoints</li>
 *   <li>Super-admins can access admin-only endpoints</li>
 *   <li>Unauthenticated requests are rejected with 401</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(EmbeddedLdapTestConfig.class)
class LdapIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Login endpoint (/api/v1/auth/login)
    // ------------------------------------------------------------------

    @Test
    void loginWithValidLdapCredentialsReturnsJwt() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "user1", "password", "password1"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.token_type", is("Bearer")));
    }

    @Test
    void loginWithInvalidPasswordReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "user1", "password", "wrong"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownUserReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "nonexistent", "password", "password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // HTTP Basic – access control
    // ------------------------------------------------------------------

    @Test
    void authenticatedUserCanAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems")
                        .with(httpBasic("user1", "password1")))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                        .with(httpBasic("user1", "password1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUserCannotAccessAdminEndpoints() throws Exception {
        // admin1 has ROLE_MY_APP_ADMIN but not ROLE_MY_APP_SUPERADMIN
        mockMvc.perform(get("/api/v1/admin/roles")
                        .with(httpBasic("admin1", "admin123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void superadminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                        .with(httpBasic("superadmin1", "super123")))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems"))
                .andExpect(status().isUnauthorized());
    }
}
