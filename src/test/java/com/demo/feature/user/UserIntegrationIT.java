package com.demo.feature.user;

import com.demo.testsupport.AsAdminMockMvc;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class UserIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto createUser(String username) throws Exception {
        UserDto dto = new UserDto();
        dto.setUsername(username);
        dto.setEmail(username + "@example.com");

        String body = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(body, UserDto.class);
    }

    @Test
    void testListUsersIsPaged() throws Exception {
        createUser("page.user.a." + System.nanoTime());
        createUser("page.user.b." + System.nanoTime());

        mockMvc.perform(get("/api/v1/users")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalPages", is(greaterThanOrEqualTo(2))));
    }

    @Test
    void testProfileFieldsRoundTrip() throws Exception {
        String username = "profile.user." + System.nanoTime();
        UserDto dto = new UserDto();
        dto.setUsername(username);
        dto.setEmail(username + "@example.com");
        dto.setFirstName("Grace");
        dto.setLastName("Hopper");

        String body = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("Grace")))
                .andExpect(jsonPath("$.lastName", is("Hopper")))
                .andReturn().getResponse().getContentAsString();
        UserDto created = objectMapper.readValue(body, UserDto.class);

        created.setLastName("Brewster Murray Hopper");
        mockMvc.perform(put("/api/v1/users/" + created.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(created))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Grace")))
                .andExpect(jsonPath("$.lastName", is("Brewster Murray Hopper")));

        mockMvc.perform(get("/api/v1/users/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName", is("Brewster Murray Hopper")));
    }

    @Test
    void testDirectoryLoginProvisionsProfileFromLdapEntry() throws Exception {
        // user1's first call provisions their row from the directory entry
        mockMvc.perform(get("/api/v1/departments").with(AsAdminMockMvc.asUser("user1", "password1")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/filter").param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].email", is("user1@example.com")))
                .andExpect(jsonPath("$.content[0].firstName", is("User")))
                .andExpect(jsonPath("$.content[0].lastName", is("One")));
    }

    @Test
    void testListUsersDefaultPaging() throws Exception {
        String username = "page.user.default." + System.nanoTime();
        createUser(username);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[*].username", hasItem(username)));
    }
}
