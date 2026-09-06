package com.demo.feature.user;

import com.demo.feature.department.DepartmentDto;
import com.demo.feature.security.rbac.role.Operation;
import com.demo.feature.security.rbac.role.PermissionDto;
import com.demo.feature.security.rbac.assignment.RoleAssignmentDto;
import com.demo.feature.security.rbac.role.RoleDto;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
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
 * The worked example from the README: a <em>Department User</em> role that may
 * read {@code username}, {@code email}, {@code firstName}, {@code lastName} and
 * update {@code firstName}, {@code lastName} of users in its own department —
 * granted to directory user {@code user1} for the IT department.
 *
 * <p>Set-up is done as {@code admin} (the default credentials of
 * {@link AsAdminMockMvc}); the assertions are made as {@code user1}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class UserRbacIT {

    private static final RequestPostProcessor USER1 = asUser("user1", "password1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long itId;
    private Long aliceId;   // in IT
    private Long bobId;     // in no department
    private Long carolId;   // in HR

    private String json(Object value) {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }

    private <T> T create(String url, Object body, Class<T> type) throws Exception {
        String response = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(body)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, type);
    }

    private Long createUser(String username, String first, String last, Long... departmentIds) throws Exception {
        UserDto dto = new UserDto();
        dto.setUsername(username);
        dto.setEmail(username + "@example.com");
        dto.setFirstName(first);
        dto.setLastName(last);
        dto.setDepartmentIds(List.of(departmentIds));
        return create("/api/v1/users", dto, UserDto.class).getId();
    }

    @BeforeEach
    void setUp() throws Exception {
        long stamp = System.nanoTime();
        itId = create("/api/v1/departments", DepartmentDto.builder().name("IT-" + stamp).build(), DepartmentDto.class).getId();
        Long hrId = create("/api/v1/departments", DepartmentDto.builder().name("HR-" + stamp).build(), DepartmentDto.class).getId();

        aliceId = createUser("alice." + stamp, "Alice", "Smith", itId);
        bobId = createUser("bob." + stamp, "Bob", "Builder");
        carolId = createUser("carol." + stamp, "Carol", "Danvers", hrId);
        // user1 (a directory account) is created here, in IT, so its later logins
        // find this row rather than provisioning a department-less one.
        Long user1Id = createUser("user1", "User", "One", itId);

        RoleDto departmentUser = create("/api/v1/roles", RoleDto.builder()
            .name("Department User " + stamp)
            .permissions(List.of(
                perm("username", Operation.READ),
                perm("email", Operation.READ),
                perm("firstName", Operation.READ),
                perm("lastName", Operation.READ),
                perm("firstName", Operation.UPDATE),
                perm("lastName", Operation.UPDATE)))
            .build(), RoleDto.class);

        create("/api/v1/users/" + user1Id + "/role-assignments",
            RoleAssignmentDto.builder().roleId(departmentUser.getId()).departmentId(itId).build(),
            RoleAssignmentDto.class);
    }

    private static PermissionDto perm(String field, Operation op) {
        return PermissionDto.builder().entity("User").field(field).operation(op).build();
    }

    @Test
    void listShowsOnlyOwnDepartmentAndOnlyReadableFields() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(2)))
            .andExpect(jsonPath("$.content[*].firstName", containsInAnyOrder("Alice", "User")))
            .andExpect(jsonPath("$.content[0].id").exists())
            .andExpect(jsonPath("$.content[0].username").exists())
            .andExpect(jsonPath("$.content[0].email").exists())
            .andExpect(jsonPath("$.content[0].departmentIds").doesNotExist())
            .andExpect(jsonPath("$.content[0].departments").doesNotExist())
            .andExpect(jsonPath("$.content[0].managerId").doesNotExist())
            .andExpect(jsonPath("$.content[0].roleAssignments").doesNotExist());

        // The filter endpoint is scoped the same way: carol is in HR
        mockMvc.perform(get("/api/v1/users/filter").param("username", "carol").with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void getByIdIsMaskedInsideTheDepartmentAndForbiddenOutsideIt() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + aliceId).with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(aliceId.intValue())))
            .andExpect(jsonPath("$.firstName", is("Alice")))
            .andExpect(jsonPath("$.email", containsString("alice.")))
            .andExpect(jsonPath("$.departmentIds").doesNotExist());

        mockMvc.perform(get("/api/v1/users/" + bobId).with(USER1))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title", is("Forbidden")))
            .andExpect(jsonPath("$.detail", containsString("requires a global grant")));

        mockMvc.perform(get("/api/v1/users/" + carolId).with(USER1))
            .andExpect(status().isForbidden());
    }

    @Test
    void getThenPutRoundTripChangesOnlyThePermittedFieldAndKeepsTheRest() throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/" + aliceId).with(USER1))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        UserDto asSeenByUser1 = objectMapper.readValue(body, UserDto.class);
        asSeenByUser1.setLastName("Jones");

        mockMvc.perform(put("/api/v1/users/" + aliceId).with(USER1)
                .contentType(MediaType.APPLICATION_JSON).content(json(asSeenByUser1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastName", is("Jones")))
            .andExpect(jsonPath("$.departmentIds").doesNotExist());

        // As admin: the department membership user1 could not see survived the PUT
        mockMvc.perform(get("/api/v1/users/" + aliceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastName", is("Jones")))
            .andExpect(jsonPath("$.departmentIds", hasSize(1)))
            .andExpect(jsonPath("$.departmentIds[0]", is(itId.intValue())));
    }

    @Test
    void changingAFieldWithoutUpdateGrantIs403NamingTheField() throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/" + aliceId).with(USER1))
            .andReturn().getResponse().getContentAsString();
        UserDto dto = objectMapper.readValue(body, UserDto.class);
        dto.setEmail("alice.new@example.com");

        mockMvc.perform(put("/api/v1/users/" + aliceId).with(USER1)
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("UPDATE User field(s) [email]")));

        // Unchanged
        mockMvc.perform(get("/api/v1/users/" + aliceId))
            .andExpect(jsonPath("$.email", containsString("alice.")))
            .andExpect(jsonPath("$.email", containsString("@example.com")));
    }

    @Test
    void updatingAUserOutsideTheDepartmentIs403() throws Exception {
        UserDto dto = new UserDto();
        dto.setUsername("carol.renamed");
        dto.setEmail("carol@example.com");

        mockMvc.perform(put("/api/v1/users/" + carolId).with(USER1)
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createAndDeleteAreForbiddenWithoutGrants() throws Exception {
        UserDto dto = new UserDto();
        dto.setUsername("newbie");
        dto.setEmail("newbie@example.com");
        dto.setDepartmentIds(List.of(itId));

        mockMvc.perform(post("/api/v1/users").with(USER1)
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("CREATE User")));

        mockMvc.perform(delete("/api/v1/users/" + aliceId).with(USER1))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users/" + aliceId)).andExpect(status().isOk());
    }

    @Test
    void aUserWithNoGrantsSeesNothingAndCannotReadThemself() throws Exception {
        RequestPostProcessor user2 = asUser("user2", "password2");

        mockMvc.perform(get("/api/v1/users").with(user2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(0)));

        // user2 was just provisioned by that call; find its id as admin
        String body = mockMvc.perform(get("/api/v1/users/filter").param("username", "user2"))
            .andExpect(jsonPath("$.totalElements", is(1)))
            .andReturn().getResponse().getContentAsString();
        int user2Id = objectMapper.readTree(body).get("content").get(0).get("id").asInt();

        mockMvc.perform(get("/api/v1/users/" + user2Id).with(user2)).andExpect(status().isForbidden());
    }

    @Test
    void superAdminStillSeesAndDoesEverything() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + bobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").exists())
            .andExpect(jsonPath("$.departmentIds", hasSize(0)))
            .andExpect(jsonPath("$.roleAssignments", hasSize(0)));

        mockMvc.perform(delete("/api/v1/users/" + bobId)).andExpect(status().isNoContent());
    }
}
