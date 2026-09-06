package com.demo.feature.security.rbac;

import com.demo.feature.department.DepartmentDto;
import com.demo.feature.user.UserDto;
import com.demo.testsupport.AsAdminMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

import static com.demo.testsupport.AsAdminMockMvc.asUser;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The RBAC endpoints are themselves governed by RBAC: roles need a global
 * grant, and role assignments are scoped to the department they hand out.
 * {@code user1} is a department-scoped grant manager for IT; {@code user2} has
 * nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class RbacManagementIT {

    private static final RequestPostProcessor USER1 = asUser("user1", "password1");
    private static final RequestPostProcessor USER2 = asUser("user2", "password2");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long itId;
    private Long hrId;
    private Long aliceId;
    private Long user1Id;
    private Long departmentUserRoleId;
    private Long superAdminRoleId;

    private String json(Object value) {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }

    private <T> T create(String url, Object body, Class<T> type) throws Exception {
        String response = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(body)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, type);
    }

    private Long createUser(String username, Long... departmentIds) throws Exception {
        UserDto dto = new UserDto();
        dto.setUsername(username);
        dto.setEmail(username + "@example.com");
        dto.setDepartmentIds(List.of(departmentIds));
        return create("/api/v1/users", dto, UserDto.class).getId();
    }

    private static PermissionDto perm(String entity, String field, Operation op) {
        return PermissionDto.builder().entity(entity).field(field).operation(op).build();
    }

    @BeforeEach
    void setUp() throws Exception {
        long stamp = System.nanoTime();
        itId = create("/api/v1/departments", DepartmentDto.builder().name("IT-" + stamp).build(), DepartmentDto.class).getId();
        hrId = create("/api/v1/departments", DepartmentDto.builder().name("HR-" + stamp).build(), DepartmentDto.class).getId();
        aliceId = createUser("alice." + stamp, itId);
        user1Id = createUser("user1", itId);
        createUser("user2");

        departmentUserRoleId = create("/api/v1/roles", RoleDto.builder().name("Department User " + stamp)
            .permissions(List.of(perm("User", "firstName", Operation.READ))).build(), RoleDto.class).getId();

        String roles = mockMvc.perform(get("/api/v1/roles").param("size", "100"))
            .andReturn().getResponse().getContentAsString();
        superAdminRoleId = objectMapper.readTree(roles).get("content").valueStream()
            .filter(node -> node.get("system").asBoolean())
            .findFirst().orElseThrow().get("id").asLong();

        RoleDto grantManager = create("/api/v1/roles", RoleDto.builder().name("Grant Manager " + stamp)
            .permissions(List.of(
                perm("RoleAssignment", "*", Operation.CREATE),
                perm("RoleAssignment", "*", Operation.READ),
                perm("RoleAssignment", "*", Operation.DELETE)))
            .build(), RoleDto.class);
        create("/api/v1/users/" + user1Id + "/role-assignments",
            RoleAssignmentDto.builder().roleId(grantManager.getId()).departmentId(itId).build(),
            RoleAssignmentDto.class);
    }

    @Test
    void userWithoutGrantsCannotTouchRolesOrGrantThemselvesAnything() throws Exception {
        mockMvc.perform(get("/api/v1/roles").with(USER2))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("READ Role (requires a global grant)")));

        mockMvc.perform(post("/api/v1/roles").with(USER2).contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleDto.builder().name("Sneaky").build())))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/roles/" + departmentUserRoleId + "/permissions").with(USER2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(List.of(perm("*", "*", Operation.READ)))))
            .andExpect(status().isForbidden());

        String user2Body = mockMvc.perform(get("/api/v1/users/filter").param("username", "user2"))
            .andReturn().getResponse().getContentAsString();
        long user2Id = objectMapper.readTree(user2Body).get("content").get(0).get("id").asLong();

        mockMvc.perform(post("/api/v1/users/" + user2Id + "/role-assignments").with(USER2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(superAdminRoleId).build())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("CREATE RoleAssignment (requires a global grant)")));
    }

    @Test
    void departmentScopedGrantManagerCanGrantWithinTheDepartmentOnly() throws Exception {
        // Within IT: allowed
        mockMvc.perform(post("/api/v1/users/" + aliceId + "/role-assignments").with(USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(departmentUserRoleId).departmentId(itId).build())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.departmentId", is(itId.intValue())));

        // In HR: not their department
        mockMvc.perform(post("/api/v1/users/" + aliceId + "/role-assignments").with(USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(departmentUserRoleId).departmentId(hrId).build())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("department(s) [" + hrId + "]")));

        // Globally: needs a global grant
        mockMvc.perform(post("/api/v1/users/" + aliceId + "/role-assignments").with(USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(RoleAssignmentDto.builder().roleId(departmentUserRoleId).build())))
            .andExpect(status().isForbidden());

        // Roles themselves stay out of reach
        mockMvc.perform(get("/api/v1/roles/" + departmentUserRoleId).with(USER1))
            .andExpect(status().isForbidden());
    }

    @Test
    void listingShowsOnlyGrantsTheCallerMayReadAndRevokeIsScopedTheSameWay() throws Exception {
        // Admin gives alice one IT grant and one global grant
        RoleAssignmentDto inIt = create("/api/v1/users/" + aliceId + "/role-assignments",
            RoleAssignmentDto.builder().roleId(departmentUserRoleId).departmentId(itId).build(), RoleAssignmentDto.class);
        RoleAssignmentDto global = create("/api/v1/users/" + aliceId + "/role-assignments",
            RoleAssignmentDto.builder().roleId(departmentUserRoleId).build(), RoleAssignmentDto.class);

        mockMvc.perform(get("/api/v1/users/" + aliceId + "/role-assignments").with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(inIt.getId().intValue())));

        mockMvc.perform(delete("/api/v1/users/" + aliceId + "/role-assignments/" + global.getId()).with(USER1))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/users/" + aliceId + "/role-assignments/" + inIt.getId()).with(USER1))
            .andExpect(status().isNoContent());

        // Admin sees the remaining global grant
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/role-assignments"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(global.getId().intValue())));
    }

    @Test
    void superAdminRoleIsLockedEvenForAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/" + superAdminRoleId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail", containsString("cannot be deleted")));

        mockMvc.perform(put("/api/v1/roles/" + superAdminRoleId + "/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(List.of(perm("User", "*", Operation.READ)))))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/roles/" + superAdminRoleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.system", is(true)))
            .andExpect(jsonPath("$.permissions", hasSize(4)));
    }
}
