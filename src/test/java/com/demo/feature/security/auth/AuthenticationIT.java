package com.demo.feature.security.auth;

import com.demo.feature.user.User;
import com.demo.feature.user.UserRepository;
import com.demo.testsupport.AsAdminMockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.demo.testsupport.AsAdminMockMvc.anonymous;
import static com.demo.testsupport.AsAdminMockMvc.asUser;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level authentication against the embedded LDAP server: who gets past the
 * front door, what a rejection looks like, and that a first login provisions the
 * caller's {@code users} row.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class AuthenticationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void apiWithoutCredentialsIsUnauthorizedAsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/departments").with(anonymous()))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Basic realm=")))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.title", is("Unauthorized")))
            .andExpect(jsonPath("$.instance", is("/api/v1/departments")));
    }

    @Test
    void apiWithWrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/departments").with(asUser("user1", "definitely-wrong")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void apiWithUnknownUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/departments").with(asUser("nobody", "password")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void apiWithValidDirectoryCredentialsIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
            .andExpect(status().isOk());
    }

    @Test
    void firstLoginProvisionsTheUsersRowFromTheDirectoryEntry() throws Exception {
        assertTrue(userRepository.findByUsername("user2").isEmpty(), "precondition: user2 not yet provisioned");

        mockMvc.perform(get("/api/v1/departments").with(asUser("user2", "password2")))
            .andExpect(status().isOk());

        User provisioned = userRepository.findByUsername("user2").orElseThrow();
        assertEquals("user2@example.com", provisioned.getEmail());
        assertEquals("User", provisioned.getFirstName());
        assertEquals("Two", provisioned.getLastName());
    }

    @Test
    void secondLoginReusesTheProvisionedRow() throws Exception {
        mockMvc.perform(get("/api/v1/departments").with(asUser("user3", "password3")))
            .andExpect(status().isOk());
        Long firstId = userRepository.findByUsername("user3").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/departments").with(asUser("user3", "password3")))
            .andExpect(status().isOk());

        assertEquals(firstId, userRepository.findByUsername("user3").orElseThrow().getId());
        assertEquals(1, userRepository.findAll().stream().filter(u -> "user3".equals(u.getUsername())).count());
    }

    @Test
    void operationalEndpointsStayOpen() throws Exception {
        mockMvc.perform(get("/actuator/health").with(anonymous()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs").with(anonymous()))
            .andExpect(status().isOk());
    }
}
