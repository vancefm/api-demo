package com.demo.application.user;

import com.demo.application.security.auth.RoleRepository;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "MY_APP_USER")
class UserManagementIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role testRole;
    private UserDto testDto;

    @BeforeEach
    void setUp() {
        testRole = roleRepository.findByName("MY_APP_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("MY_APP_USER")
                        .description("Test role")
                        .build()));

        testDto = new UserDto();
        testDto.setUsername("john.doe");
        testDto.setEmail("john.doe@example.com");
        testDto.setDepartment("IT");
        testDto.setRoleId(testRole.getId());
    }

    @Test
    void testCreateAndRetrieveUser() throws Exception {
        // Create
        String responseBody = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("john.doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdDto = objectMapper.readValue(responseBody, UserDto.class);

        // Retrieve by ID
        mockMvc.perform(get("/api/v1/users/" + createdDto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("john.doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.department", is("IT")))
                .andExpect(jsonPath("$.roleName", is("MY_APP_USER")));
    }

    @Test
    void testGetAllUsers() throws Exception {
        // Create two users
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated());

        UserDto secondDto = new UserDto();
        secondDto.setUsername("jane.doe");
        secondDto.setEmail("jane.doe@example.com");
        secondDto.setDepartment("HR");
        secondDto.setRoleId(testRole.getId());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(secondDto))))
                .andExpect(status().isCreated());

        // Get all
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testUpdateUser() throws Exception {
        // Create
        String responseBody = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdDto = objectMapper.readValue(responseBody, UserDto.class);

        // Update
        createdDto.setEmail("john.updated@example.com");
        createdDto.setDepartment("Engineering");

        mockMvc.perform(put("/api/v1/users/" + createdDto.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(createdDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("john.updated@example.com")))
                .andExpect(jsonPath("$.department", is("Engineering")));
    }

    @Test
    void testDeleteUser() throws Exception {
        // Create
        String responseBody = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdDto = objectMapper.readValue(responseBody, UserDto.class);

        // Delete
        mockMvc.perform(delete("/api/v1/users/" + createdDto.getId()))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/v1/users/" + createdDto.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateUser_DuplicateUsername() throws Exception {
        // Create first user
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated());

        // Attempt to create duplicate
        UserDto duplicateDto = new UserDto();
        duplicateDto.setUsername("john.doe");
        duplicateDto.setEmail("different@example.com");
        duplicateDto.setDepartment("HR");
        duplicateDto.setRoleId(testRole.getId());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(duplicateDto))))
                .andExpect(status().isConflict());
    }

    @Test
    void testCreateUser_DuplicateEmail() throws Exception {
        // Create first user
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated());

        // Attempt to create with duplicate email
        UserDto duplicateDto = new UserDto();
        duplicateDto.setUsername("different.user");
        duplicateDto.setEmail("john.doe@example.com");
        duplicateDto.setDepartment("HR");
        duplicateDto.setRoleId(testRole.getId());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(duplicateDto))))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateUser_ValidationError() throws Exception {
        UserDto invalidDto = new UserDto();
        // All required fields are blank/null

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalidDto))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUserWithManager() throws Exception {
        // Create manager
        String managerResponse = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto managerDto = objectMapper.readValue(managerResponse, UserDto.class);

        // Create user with manager
        UserDto employeeDto = new UserDto();
        employeeDto.setUsername("employee");
        employeeDto.setEmail("employee@example.com");
        employeeDto.setDepartment("IT");
        employeeDto.setRoleId(testRole.getId());
        employeeDto.setManagerId(managerDto.getId());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(employeeDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managerId", is(managerDto.getId().intValue())));
    }
}
