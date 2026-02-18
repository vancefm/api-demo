package com.demo.application.user;

import com.demo.domain.user.UserDto;
import com.demo.shared.service.EmailNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "MY_APP_USER")
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testDto;

    @BeforeEach
    void setUp() {
        testDto = new UserDto();
        testDto.setId(1L);
        testDto.setUsername("john.doe");
        testDto.setEmail("john.doe@example.com");
        testDto.setDepartment("IT");
        testDto.setRoleId(1L);
        testDto.setRoleName("MY_APP_USER");
        testDto.setManagerId(null);
    }

    @Test
    void testCreateUser() throws Exception {
        when(userManagementService.createUser(any(UserDto.class))).thenReturn(testDto);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("john.doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.department", is("IT")));

        verify(userManagementService, times(1)).createUser(any(UserDto.class));
    }

    @Test
    void testCreateUser_InvalidInput_MissingUsername() throws Exception {
        UserDto invalidDto = new UserDto();
        invalidDto.setEmail("john.doe@example.com");
        invalidDto.setDepartment("IT");
        invalidDto.setRoleId(1L);
        // username is blank

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalidDto))))
                .andExpect(status().isBadRequest());

        verify(userManagementService, never()).createUser(any(UserDto.class));
    }

    @Test
    void testCreateUser_InvalidInput_MissingEmail() throws Exception {
        UserDto invalidDto = new UserDto();
        invalidDto.setUsername("john.doe");
        invalidDto.setDepartment("IT");
        invalidDto.setRoleId(1L);
        // email is blank

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalidDto))))
                .andExpect(status().isBadRequest());

        verify(userManagementService, never()).createUser(any(UserDto.class));
    }

    @Test
    void testCreateUser_InvalidInput_MissingRoleId() throws Exception {
        UserDto invalidDto = new UserDto();
        invalidDto.setUsername("john.doe");
        invalidDto.setEmail("john.doe@example.com");
        invalidDto.setDepartment("IT");
        // roleId is null

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalidDto))))
                .andExpect(status().isBadRequest());

        verify(userManagementService, never()).createUser(any(UserDto.class));
    }

    @Test
    void testGetAllUsers() throws Exception {
        UserDto secondDto = new UserDto();
        secondDto.setId(2L);
        secondDto.setUsername("jane.doe");
        secondDto.setEmail("jane.doe@example.com");
        secondDto.setDepartment("HR");
        secondDto.setRoleId(1L);
        secondDto.setRoleName("MY_APP_USER");

        List<UserDto> users = Arrays.asList(testDto, secondDto);
        when(userManagementService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("john.doe")))
                .andExpect(jsonPath("$[1].username", is("jane.doe")));

        verify(userManagementService, times(1)).getAllUsers();
    }

    @Test
    void testGetUserById() throws Exception {
        when(userManagementService.getUserById(1L)).thenReturn(testDto);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("john.doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")));

        verify(userManagementService, times(1)).getUserById(1L);
    }

    @Test
    void testUpdateUser() throws Exception {
        UserDto updatedDto = new UserDto();
        updatedDto.setId(1L);
        updatedDto.setUsername("john.doe");
        updatedDto.setEmail("john.updated@example.com");
        updatedDto.setDepartment("Engineering");
        updatedDto.setRoleId(1L);
        updatedDto.setRoleName("MY_APP_USER");

        when(userManagementService.updateUser(eq(1L), any(UserDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(updatedDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("john.updated@example.com")))
                .andExpect(jsonPath("$.department", is("Engineering")));

        verify(userManagementService, times(1)).updateUser(eq(1L), any(UserDto.class));
    }

    @Test
    void testDeleteUser() throws Exception {
        doNothing().when(userManagementService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());

        verify(userManagementService, times(1)).deleteUser(1L);
    }
}
