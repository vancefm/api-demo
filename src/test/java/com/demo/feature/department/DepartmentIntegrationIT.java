package com.demo.feature.department;

import com.demo.feature.user.UserDto;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
        // requests, unlike production where each request gets its own. Detach
        // the managed User before the delete so the native join-table delete
        // doesn't conflict with its stale in-memory departments collection.
        entityManager.flush();
        entityManager.clear();

        // Delete the department — must succeed despite the assignment (cascade-dissociate)
        mockMvc.perform(delete("/api/v1/departments/" + department.getId()))
                .andExpect(status().isNoContent());

        // Re-fetch the user: the deleted department is silently unlinked
        mockMvc.perform(get("/api/v1/users/" + createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments", hasSize(0)))
                .andExpect(jsonPath("$.departmentIds", hasSize(0)));
    }
}
