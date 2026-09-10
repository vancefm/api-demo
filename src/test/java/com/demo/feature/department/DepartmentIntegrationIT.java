package com.demo.feature.department;

import com.demo.feature.user.UserDto;
import com.demo.testsupport.AsAdminMockMvc;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AsAdminMockMvc.class)
class DepartmentIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private DepartmentDto createDepartment(String name) throws Exception {
        DepartmentDto dto = DepartmentDto.builder()
                .name(name)
                .description("Description of " + name)
                .build();

        String body = mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(body, DepartmentDto.class);
    }

    @Test
    void testCreateAndRetrieveDepartment() throws Exception {
        String name = "DEPT-" + System.nanoTime();
        DepartmentDto created = createDepartment(name);

        mockMvc.perform(get("/api/v1/departments/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.description", is("Description of " + name)));
    }

    @Test
    void testDuplicateNameConflict() throws Exception {
        String name = "DEPT-" + System.nanoTime();
        createDepartment(name);

        DepartmentDto duplicate = DepartmentDto.builder().name(name).build();
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(duplicate))))
                .andExpect(status().isConflict());
    }

    @Test
    void testValidationError() throws Exception {
        DepartmentDto invalid = DepartmentDto.builder().name("").build();

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalid))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListDepartments() throws Exception {
        createDepartment("DEPT-" + System.nanoTime());

        mockMvc.perform(get("/api/v1/departments")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testFilterDepartmentsByName() throws Exception {
        String name = "DEPT-" + System.nanoTime();
        DepartmentDto created = createDepartment(name);
        createDepartment("DEPT-" + System.nanoTime());

        // Unique nanoTime suffix guarantees exactly one partial-name match
        mockMvc.perform(get("/api/v1/departments/filter")
                .param("name", name.substring(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.content[0].name", is(name)));
    }

    @Test
    void testFilterUsersByDepartmentAndUsername() throws Exception {
        DepartmentDto department = createDepartment("DEPT-" + System.nanoTime());

        String memberName = "filter.member." + System.nanoTime();
        UserDto member = new UserDto();
        member.setUsername(memberName);
        member.setEmail(memberName + "@example.com");
        member.setDepartmentIds(List.of(department.getId()));

        String outsiderName = "filter.outsider." + System.nanoTime();
        UserDto outsider = new UserDto();
        outsider.setUsername(outsiderName);
        outsider.setEmail(outsiderName + "@example.com");

        for (UserDto dto : List.of(member, outsider)) {
            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                    .andExpect(status().isCreated());
        }

        // Department membership: only the member matches
        mockMvc.perform(get("/api/v1/users/filter")
                .param("departmentId", department.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].username", is(memberName)));

        // Partial username match: only the outsider matches
        mockMvc.perform(get("/api/v1/users/filter")
                .param("username", outsiderName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].username", is(outsiderName)));
    }

    @Test
    void testUpdateDepartment() throws Exception {
        DepartmentDto created = createDepartment("DEPT-" + System.nanoTime());

        created.setDescription("Updated description");
        mockMvc.perform(put("/api/v1/departments/" + created.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(created))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated description")));
    }

    @Test
    void testDeleteDepartment() throws Exception {
        DepartmentDto created = createDepartment("DEPT-" + System.nanoTime());

        mockMvc.perform(delete("/api/v1/departments/" + created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/departments/" + created.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/departments/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUserWithUnknownDepartmentIdRejected() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setUsername("dept.user." + System.nanoTime());
        userDto.setEmail("dept.user@example.com");
        userDto.setDepartmentIds(List.of(999999L));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(userDto))))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDepartmentDissociatesUsers() throws Exception {
        DepartmentDto department = createDepartment("DEPT-" + System.nanoTime());

        // Create a user assigned to the department via the API
        UserDto userDto = new UserDto();
        String username = "dept.user." + System.nanoTime();
        userDto.setUsername(username);
        userDto.setEmail(username + "@example.com");
        userDto.setDepartmentIds(List.of(department.getId()));

        String body = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(userDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departments", hasSize(1)))
                .andExpect(jsonPath("$.departments[0].id", is(department.getId().intValue())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser = objectMapper.readValue(body, UserDto.class);

        // This @Transactional test shares one persistence context across all
        // requests, unlike production where each request commits its own.
        entityManager.flush();
        entityManager.clear();

        // Delete the department — must succeed despite the assignment (cascade-dissociate)
        mockMvc.perform(delete("/api/v1/departments/" + department.getId()))
                .andExpect(status().isNoContent());

        // Dissociation happens in the database via ON DELETE CASCADE, which is
        // invisible to the persistence context. Flush so the DELETE actually
        // reaches the database (firing the cascade), then clear so the re-read
        // below goes to the database rather than to cached state. In production
        // the request's own commit does both.
        entityManager.flush();
        entityManager.clear();

        // Re-fetch the user: the deleted department is silently unlinked
        mockMvc.perform(get("/api/v1/users/" + createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments", hasSize(0)))
                .andExpect(jsonPath("$.departmentIds", hasSize(0)));
    }
}
