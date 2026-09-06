package com.demo.feature.security.rbac.role;

import com.demo.integration.mail.EmailNotificationService;
import com.demo.platform.exception.ConflictException;
import com.demo.platform.exception.InvalidRequestException;
import com.demo.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService service;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private RoleDto testDto;
    private PermissionDto readFirstName;

    @BeforeEach
    void setUp() {
        readFirstName = PermissionDto.builder().id(7L).entity("User").field("firstName").operation(Operation.READ).build();
        testDto = RoleDto.builder().id(1L).name("Department User").description("desc").system(false)
            .permissions(List.of(readFirstName)).build();
    }

    private String json(Object value) {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }

    @Test
    void createRole() throws Exception {
        when(service.createRole(any(RoleDto.class))).thenReturn(testDto);

        mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON).content(json(testDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is("Department User")))
            .andExpect(jsonPath("$.system", is(false)))
            .andExpect(jsonPath("$.permissions", hasSize(1)))
            .andExpect(jsonPath("$.permissions[0].operation", is("READ")));
    }

    @Test
    void createRole_blankNameIs400() throws Exception {
        mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleDto.builder().name(" ").build())))
            .andExpect(status().isBadRequest());

        verify(service, never()).createRole(any());
    }

    @Test
    void createRole_unknownFieldIs400ProblemDetail() throws Exception {
        when(service.createRole(any(RoleDto.class)))
            .thenThrow(new InvalidRequestException("Unknown field 'password' on entity 'User'"));

        mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON).content(json(testDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("Invalid Request")))
            .andExpect(jsonPath("$.detail", is("Unknown field 'password' on entity 'User'")));
    }

    @Test
    void getRoleById() throws Exception {
        when(service.getRoleById(1L)).thenReturn(testDto);

        mockMvc.perform(get("/api/v1/roles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void getRoleById_notFound() throws Exception {
        when(service.getRoleById(99L)).thenThrow(new ResourceNotFoundException("Role with id 99 not found"));

        mockMvc.perform(get("/api/v1/roles/99")).andExpect(status().isNotFound());
    }

    @Test
    void listRoles() throws Exception {
        when(service.getAllRoles(any())).thenReturn(new PageImpl<>(List.of(testDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void updateRole() throws Exception {
        when(service.updateRole(eq(1L), any(RoleDto.class))).thenReturn(testDto);

        mockMvc.perform(put("/api/v1/roles/1").contentType(MediaType.APPLICATION_JSON).content(json(testDto)))
            .andExpect(status().isOk());
    }

    @Test
    void deleteRole_systemRoleIs409() throws Exception {
        doThrow(new ConflictException("System role 'SuperAdmin' cannot be deleted")).when(service).deleteRole(2L);

        mockMvc.perform(delete("/api/v1/roles/2"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title", is("Conflict")));
    }

    @Test
    void deleteRole() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1")).andExpect(status().isNoContent());

        verify(service).deleteRole(1L);
    }

    @Test
    void replacePermissionsAcceptsBareArray() throws Exception {
        when(service.replacePermissions(eq(1L), anyList())).thenReturn(List.of(readFirstName));

        mockMvc.perform(put("/api/v1/roles/1/permissions").contentType(MediaType.APPLICATION_JSON)
                .content(json(List.of(readFirstName))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].field", is("firstName")));
    }

    @Test
    void addPermission_missingOperationIs400() throws Exception {
        mockMvc.perform(post("/api/v1/roles/1/permissions").contentType(MediaType.APPLICATION_JSON)
                .content(json(PermissionDto.builder().entity("User").field("email").build())))
            .andExpect(status().isBadRequest());

        verify(service, never()).addPermission(any(), any());
    }

    @Test
    void addAndRemovePermission() throws Exception {
        when(service.addPermission(eq(1L), any(PermissionDto.class))).thenReturn(readFirstName);

        mockMvc.perform(post("/api/v1/roles/1/permissions").contentType(MediaType.APPLICATION_JSON)
                .content(json(readFirstName)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(7)));

        mockMvc.perform(delete("/api/v1/roles/1/permissions/7")).andExpect(status().isNoContent());
        verify(service).removePermission(1L, 7L);
    }
}
