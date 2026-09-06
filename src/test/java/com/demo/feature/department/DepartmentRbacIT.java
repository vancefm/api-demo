package com.demo.feature.department;

import com.demo.feature.security.rbac.role.Operation;
import com.demo.feature.security.rbac.role.PermissionDto;
import com.demo.feature.security.rbac.assignment.RoleAssignmentDto;
import com.demo.feature.security.rbac.role.RoleDto;
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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A department is its own scope: {@code user1} holds a role in IT that may read
 * {@code name} and {@code description} and update {@code description}, so it can
 * see and describe IT — and nothing else.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class DepartmentRbacIT {

    private static final RequestPostProcessor USER1 = asUser("user1", "password1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long itId;
    private Long hrId;

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
        itId = create("/api/v1/departments",
            DepartmentDto.builder().name("IT-" + stamp).description("Information Technology").build(),
            DepartmentDto.class).getId();
        hrId = create("/api/v1/departments",
            DepartmentDto.builder().name("HR-" + stamp).description("Human Resources").build(),
            DepartmentDto.class).getId();

        UserDto user1 = new UserDto();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        Long user1Id = create("/api/v1/users", user1, UserDto.class).getId();

        RoleDto describer = create("/api/v1/roles", RoleDto.builder()
            .name("Department Describer " + stamp)
            .permissions(List.of(
                PermissionDto.builder().entity("Department").field("name").operation(Operation.READ).build(),
                PermissionDto.builder().entity("Department").field("description").operation(Operation.READ).build(),
                PermissionDto.builder().entity("Department").field("description").operation(Operation.UPDATE).build()))
            .build(), RoleDto.class);

        create("/api/v1/users/" + user1Id + "/role-assignments",
            RoleAssignmentDto.builder().roleId(describer.getId()).departmentId(itId).build(),
            RoleAssignmentDto.class);
    }

    @Test
    void listAndFilterShowOnlyTheScopedDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/departments").with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(1)))
            .andExpect(jsonPath("$.content[0].id", is(itId.intValue())))
            .andExpect(jsonPath("$.content[0].description", is("Information Technology")));

        mockMvc.perform(get("/api/v1/departments/filter").param("name", "HR-").with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void getByIdInsideScopeWorksOutsideIs403() throws Exception {
        mockMvc.perform(get("/api/v1/departments/" + itId).with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", containsString("IT-")));

        mockMvc.perform(get("/api/v1/departments/" + hrId).with(USER1))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("READ Department in department(s) [" + hrId + "]")));
    }

    @Test
    void mayUpdateDescriptionButNotName() throws Exception {
        String body = mockMvc.perform(get("/api/v1/departments/" + itId).with(USER1))
            .andReturn().getResponse().getContentAsString();
        DepartmentDto dto = objectMapper.readValue(body, DepartmentDto.class);

        dto.setDescription("IT and Operations");
        mockMvc.perform(put("/api/v1/departments/" + itId).with(USER1)
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description", is("IT and Operations")));

        dto.setName("Renamed");
        mockMvc.perform(put("/api/v1/departments/" + itId).with(USER1)
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("UPDATE Department field(s) [name]")));
    }

    @Test
    void createNeedsAGlobalGrantAndDeleteIsNotGranted() throws Exception {
        mockMvc.perform(post("/api/v1/departments").with(USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(DepartmentDto.builder().name("OPS-" + System.nanoTime()).build())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("requires a global grant")));

        mockMvc.perform(delete("/api/v1/departments/" + itId).with(USER1))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/departments/" + itId)).andExpect(status().isOk());
    }
}
