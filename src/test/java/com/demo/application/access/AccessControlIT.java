package com.demo.application.access;

import com.demo.application.computersystem.ComputerSystemRepository;
import com.demo.application.department.DepartmentRepository;
import com.demo.application.security.auth.RoleRepository;
import com.demo.application.user.UserRepository;
import com.demo.domain.computersystem.ComputerSystem;
import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.domain.department.Department;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.User;
import com.demo.domain.user.UserDto;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Department- and field-level access control, exercised as a department-scoped admin and
 * a self-service user (the existing ITs run as a superadmin and would bypass scoping).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessControlIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private ComputerSystemRepository computerSystemRepository;

    private ComputerSystem systemA;
    private ComputerSystem systemB;
    private User owner;

    @BeforeEach
    void setUp() {
        Department deptA = departmentRepository.save(Department.builder().name("Dept-A").build());
        Department deptB = departmentRepository.save(Department.builder().name("Dept-B").build());

        Role adminRole = roleRepository.findByName("MY_APP_ADMIN").orElseThrow();
        Role userRole = roleRepository.findByName("MY_APP_USER").orElseThrow();

        // Admin scoped to Dept A only.
        userRepository.save(User.builder()
                .username("dept-a-admin").email("a-admin@example.com")
                .departments(new HashSet<>(Set.of(deptA)))
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        owner = userRepository.save(User.builder()
                .username("owner").email("owner@example.com")
                .departments(new HashSet<>(Set.of(deptA)))
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        User other = userRepository.save(User.builder()
                .username("other").email("other@example.com")
                .departments(new HashSet<>(Set.of(deptB)))
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        systemA = computerSystemRepository.save(ComputerSystem.builder()
                .hostname("SYS-A").manufacturer("Dell").model("R750")
                .systemUser(owner).departments(new HashSet<>(Set.of(deptA)))
                .macAddress("00:1A:2B:3C:4D:01").ipAddress("10.0.0.1").networkName("NET-A")
                .build());

        systemB = computerSystemRepository.save(ComputerSystem.builder()
                .hostname("SYS-B").manufacturer("Dell").model("R750")
                .systemUser(other).departments(new HashSet<>(Set.of(deptB)))
                .macAddress("00:1A:2B:3C:4D:02").ipAddress("10.0.0.2").networkName("NET-B")
                .build());
    }

    @Test
    @WithMockUser(username = "dept-a-admin", roles = "MY_APP_ADMIN")
    void adminSeesOnlyOwnDepartmentInList() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].hostname", is("SYS-A")));
    }

    @Test
    @WithMockUser(username = "dept-a-admin", roles = "MY_APP_ADMIN")
    void adminCannotReadSystemInOtherDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems/" + systemB.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dept-a-admin", roles = "MY_APP_ADMIN")
    void adminCanReadSystemInOwnDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems/" + systemA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname", is("SYS-A")));
    }

    @Test
    @WithMockUser(username = "dept-a-admin", roles = "MY_APP_ADMIN")
    void adminCannotModifyRestrictedField() throws Exception {
        ComputerSystemDto dto = readSystemAsAdmin(systemA.getId());
        dto.setNetworkName("CHANGED-NET"); // networkName is not in the admin's writable set
        mockMvc.perform(put("/api/v1/computer-systems/" + systemA.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dept-a-admin", roles = "MY_APP_ADMIN")
    void adminCanModifyAllowedField() throws Exception {
        ComputerSystemDto dto = readSystemAsAdmin(systemA.getId());
        dto.setModel("R760"); // model is writable for admins
        mockMvc.perform(put("/api/v1/computer-systems/" + systemA.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model", is("R760")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "MY_APP_USER")
    void selfServiceUserReadsOwnSystemButNotOthers() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems/" + systemA.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/computer-systems/" + systemB.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner", roles = "MY_APP_USER")
    void selfServiceUserCannotDeleteSystem() throws Exception {
        mockMvc.perform(delete("/api/v1/computer-systems/" + systemA.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner", roles = "MY_APP_USER")
    void selfServiceUserCanEditOwnBiographicalFieldsOnly() throws Exception {
        UserDto dto = new UserDto();
        dto.setUsername("owner");
        dto.setEmail("new-email@example.com"); // biographical — writable
        dto.setDepartmentIds(owner.getDepartmentIds());
        dto.setRoleIds(Set.of(roleRepository.findByName("MY_APP_USER").orElseThrow().getId()));

        mockMvc.perform(put("/api/v1/users/" + owner.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("new-email@example.com")));

        // Attempting to change a non-biographical field (username) is rejected.
        UserDto rename = new UserDto();
        rename.setUsername("hacked");
        rename.setEmail("owner@example.com");
        rename.setDepartmentIds(owner.getDepartmentIds());
        rename.setRoleIds(Set.of(roleRepository.findByName("MY_APP_USER").orElseThrow().getId()));

        mockMvc.perform(put("/api/v1/users/" + owner.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(rename))))
                .andExpect(status().isForbidden());
    }

    private ComputerSystemDto readSystemAsAdmin(Long id) throws Exception {
        String body = mockMvc.perform(get("/api/v1/computer-systems/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, ComputerSystemDto.class);
    }
}
