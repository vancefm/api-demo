package com.demo.feature.security.rbac.role;

import com.demo.testsupport.AsAdminMockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class RoleIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object value) {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }

    private static PermissionDto perm(String entity, String field, Operation op) {
        return PermissionDto.builder().entity(entity).field(field).operation(op).build();
    }

    private RoleDto createRole(String name, List<PermissionDto> permissions) throws Exception {
        RoleDto dto = RoleDto.builder().name(name).description("Test role").permissions(permissions).build();
        String body = mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON).content(json(dto)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, RoleDto.class);
    }

    @Test
    void createRoleWithPermissionsAndReadItBack() throws Exception {
        RoleDto created = createRole("Reader-" + System.nanoTime(), List.of(
            perm("User", "firstName", Operation.READ),
            perm("User", "lastName", Operation.READ)));

        mockMvc.perform(get("/api/v1/roles/" + created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.system", is(false)))
            .andExpect(jsonPath("$.permissions", hasSize(2)))
            .andExpect(jsonPath("$.permissions[*].field", hasItem("firstName")))
            .andExpect(jsonPath("$.permissions[*].field", hasItem("lastName")));
    }

    @Test
    void replacePermissionsIsExactAndIdempotent() throws Exception {
        RoleDto created = createRole("Replace-" + System.nanoTime(), List.of(perm("User", "firstName", Operation.READ)));

        List<PermissionDto> desired = List.of(
            perm("User", "firstName", Operation.READ),
            perm("User", "email", Operation.UPDATE));
        mockMvc.perform(put("/api/v1/roles/" + created.getId() + "/permissions")
                .contentType(MediaType.APPLICATION_JSON).content(json(desired)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        // Re-submitting the same list changes nothing and does not trip the unique constraint
        mockMvc.perform(put("/api/v1/roles/" + created.getId() + "/permissions")
                .contentType(MediaType.APPLICATION_JSON).content(json(desired)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/v1/roles/" + created.getId() + "/permissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void unknownEntityOrFieldIs400() throws Exception {
        RoleDto created = createRole("Strict-" + System.nanoTime(), List.of());
        String permissions = "/api/v1/roles/" + created.getId() + "/permissions";

        mockMvc.perform(put(permissions).contentType(MediaType.APPLICATION_JSON)
                .content(json(List.of(perm("User", "password", Operation.UPDATE)))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail", containsString("password")));

        mockMvc.perform(put(permissions).contentType(MediaType.APPLICATION_JSON)
                .content(json(List.of(perm("Widget", "*", Operation.READ)))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail", containsString("Widget")));

        // Rejected as a whole: the valid grant in the same list is not stored either
        mockMvc.perform(get(permissions)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void duplicateRoleNameIs409() throws Exception {
        String name = "Dup-" + System.nanoTime();
        createRole(name, List.of());

        mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleDto.builder().name(name).build())))
            .andExpect(status().isConflict());
    }

    /**
     * Adding and removing a single grant is expressed by sending a longer or
     * shorter list — there is no add-one or remove-one route.
     */
    @Test
    void addingAndRemovingOneGrantViaTheReplaceCall() throws Exception {
        RoleDto created = createRole("Single-" + System.nanoTime(), List.of());
        String permissions = "/api/v1/roles/" + created.getId() + "/permissions";

        mockMvc.perform(put(permissions).contentType(MediaType.APPLICATION_JSON)
                .content(json(List.of(perm("ComputerSystem", "hostname", Operation.READ)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].field", is("hostname")));

        mockMvc.perform(put(permissions).contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(permissions)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void permissionsHaveNoIdOfTheirOwn() throws Exception {
        RoleDto created = createRole("Ids-" + System.nanoTime(),
            List.of(perm("User", "firstName", Operation.READ)));

        mockMvc.perform(get("/api/v1/roles/" + created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions[0].entity", is("User")))
            .andExpect(jsonPath("$.permissions[0].id").doesNotExist());
    }

    @Test
    void deleteRole() throws Exception {
        RoleDto created = createRole("Gone-" + System.nanoTime(), List.of(perm("*", "*", Operation.READ)));

        mockMvc.perform(delete("/api/v1/roles/" + created.getId())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/roles/" + created.getId())).andExpect(status().isNotFound());
    }
}
