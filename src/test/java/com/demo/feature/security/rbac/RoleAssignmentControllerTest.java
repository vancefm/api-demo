package com.demo.feature.security.rbac;

import com.demo.integration.mail.EmailNotificationService;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleAssignmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleAssignmentService service;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private final RoleAssignmentDto grant = RoleAssignmentDto.builder()
        .id(5L).userId(7L).roleId(2L).roleName("Department User").departmentId(1L).departmentName("IT").build();

    private String json(Object value) {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }

    @Test
    void list() throws Exception {
        when(service.list(7L)).thenReturn(List.of(grant));

        mockMvc.perform(get("/api/v1/users/7/role-assignments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].roleName", is("Department User")))
            .andExpect(jsonPath("$[0].departmentName", is("IT")));
    }

    @Test
    void list_unknownUserIs404() throws Exception {
        when(service.list(99L)).thenThrow(new ResourceNotFoundException("User with id 99 not found"));

        mockMvc.perform(get("/api/v1/users/99/role-assignments")).andExpect(status().isNotFound());
    }

    @Test
    void grant() throws Exception {
        when(service.grant(eq(7L), any(RoleAssignmentDto.class))).thenReturn(grant);

        mockMvc.perform(post("/api/v1/users/7/role-assignments").contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(2L).departmentId(1L).build())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(5)))
            .andExpect(jsonPath("$.userId", is(7)));
    }

    @Test
    void grant_missingRoleIdIs400() throws Exception {
        mockMvc.perform(post("/api/v1/users/7/role-assignments").contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().departmentId(1L).build())))
            .andExpect(status().isBadRequest());

        verify(service, never()).grant(any(), any());
    }

    @Test
    void grant_duplicateIs409() throws Exception {
        when(service.grant(eq(7L), any(RoleAssignmentDto.class)))
            .thenThrow(new DuplicateResourceException("already holds"));

        mockMvc.perform(post("/api/v1/users/7/role-assignments").contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(2L).build())))
            .andExpect(status().isConflict());
    }

    @Test
    void revoke() throws Exception {
        mockMvc.perform(delete("/api/v1/users/7/role-assignments/5")).andExpect(status().isNoContent());

        verify(service).revoke(7L, 5L);
    }

    @Test
    void revoke_unknownIs404() throws Exception {
        doThrow(new ResourceNotFoundException("not found")).when(service).revoke(7L, 99L);

        mockMvc.perform(delete("/api/v1/users/7/role-assignments/99")).andExpect(status().isNotFound());
    }
}
