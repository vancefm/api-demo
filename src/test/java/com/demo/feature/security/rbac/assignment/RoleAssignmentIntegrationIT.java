package com.demo.feature.security.rbac.assignment;
import com.demo.feature.security.rbac.role.PermissionDto;
import com.demo.feature.security.rbac.role.Role;
import com.demo.feature.security.rbac.role.RoleDto;
import com.demo.feature.security.rbac.role.Operation;

import com.demo.feature.department.DepartmentDto;
import com.demo.feature.user.UserDto;
import com.demo.testsupport.AsAdminMockMvc;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class RoleAssignmentIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private Long userId;
    private Long roleId;
    private Long departmentId;

    private String json(Object value) {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }

    private <T> T create(String url, Object body, Class<T> type) throws Exception {
        String response = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(body)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, type);
    }

    @BeforeEach
    void setUp() throws Exception {
        long stamp = System.nanoTime();
        departmentId = create("/api/v1/departments",
            DepartmentDto.builder().name("DEPT-" + stamp).build(), DepartmentDto.class).getId();

        UserDto user = new UserDto();
        user.setUsername("grantee." + stamp);
        user.setEmail("grantee." + stamp + "@example.com");
        userId = create("/api/v1/users", user, UserDto.class).getId();

        roleId = create("/api/v1/roles", RoleDto.builder().name("Role-" + stamp)
            .permissions(List.of(PermissionDto.builder().entity("User").field("firstName").operation(Operation.READ).build()))
            .build(), RoleDto.class).getId();
    }

    private String assignmentsUrl() {
        return "/api/v1/users/" + userId + "/role-assignments";
    }

    @Test
    void grantGlobalAndDepartmentScopedThenListAndSeeThemOnTheUser() throws Exception {
        RoleAssignmentDto global = create(assignmentsUrl(),
            RoleAssignmentDto.builder().roleId(roleId).build(), RoleAssignmentDto.class);
        RoleAssignmentDto scoped = create(assignmentsUrl(),
            RoleAssignmentDto.builder().roleId(roleId).departmentId(departmentId).build(), RoleAssignmentDto.class);

        mockMvc.perform(get(assignmentsUrl()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id", is(global.getId().intValue())))
            .andExpect(jsonPath("$[0].departmentId").doesNotExist())
            .andExpect(jsonPath("$[1].id", is(scoped.getId().intValue())))
            .andExpect(jsonPath("$[1].departmentId", is(departmentId.intValue())));

        mockMvc.perform(get("/api/v1/users/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignments", hasSize(2)))
            .andExpect(jsonPath("$.roleAssignments[1].departmentId", is(departmentId.intValue())));
    }

    @Test
    void duplicateGrantIs409AndUnknownRoleIs404() throws Exception {
        create(assignmentsUrl(), RoleAssignmentDto.builder().roleId(roleId).build(), RoleAssignmentDto.class);

        mockMvc.perform(post(assignmentsUrl()).contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(roleId).build())))
            .andExpect(status().isConflict());

        mockMvc.perform(post(assignmentsUrl()).contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(999999L).build())))
            .andExpect(status().isNotFound());

        mockMvc.perform(post(assignmentsUrl()).contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(roleId).departmentId(999999L).build())))
            .andExpect(status().isNotFound());
    }

    @Test
    void revokeRemovesTheGrant() throws Exception {
        RoleAssignmentDto grant = create(assignmentsUrl(),
            RoleAssignmentDto.builder().roleId(roleId).build(), RoleAssignmentDto.class);

        mockMvc.perform(delete(assignmentsUrl() + "/" + grant.getId())).andExpect(status().isNoContent());
        mockMvc.perform(delete(assignmentsUrl() + "/" + grant.getId())).andExpect(status().isNotFound());
        mockMvc.perform(get(assignmentsUrl())).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deletingTheRoleRemovesTheGrantFromTheUser() throws Exception {
        create(assignmentsUrl(), RoleAssignmentDto.builder().roleId(roleId).build(), RoleAssignmentDto.class);

        // This @Transactional test shares one persistence context across requests;
        // flush so the DELETE reaches the database and fires the FK cascade, then
        // clear so the re-read comes from the database (see DepartmentIntegrationIT).
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(delete("/api/v1/roles/" + roleId)).andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(assignmentsUrl())).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/users/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignments", hasSize(0)));
    }
}
