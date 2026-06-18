package com.demo.application.computersystem;

import com.demo.application.department.DepartmentRepository;
import com.demo.application.security.auth.RoleRepository;
import com.demo.application.user.UserRepository;
import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.domain.department.Department;
import com.demo.domain.security.role.Role;
import com.demo.domain.user.User;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * End-to-end tests acting as the bootstrap superadmin (ALL scope), exercising the
 * department/role-aware computer-system endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "superadmin", roles = "MY_APP_SUPERADMIN")
class ComputerSystemIntegrationIT {

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

    private ComputerSystemDto testDto;
    private Department itDept;
    private User johnDoe;
    private User janeDoe;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName("MY_APP_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("MY_APP_USER").description("Test role").build()));

        itDept = departmentRepository.save(Department.builder().name("IT").build());

        johnDoe = userRepository.save(User.builder()
                .username("john.doe")
                .email("john.doe@example.com")
                .departments(new HashSet<>(Set.of(itDept)))
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        janeDoe = userRepository.save(User.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .departments(new HashSet<>(Set.of(itDept)))
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        testDto = ComputerSystemDto.builder()
                .hostname("SERVER-001")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(johnDoe.getId())
                .departmentIds(Set.of(itDept.getId()))
                .macAddress("00:1A:2B:3C:4D:5E")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();
    }

    @Test
    void testCreateAndRetrieveComputerSystem() throws Exception {
        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.id", notNullValue()));

        mockMvc.perform(get("/api/v1/computer-systems/hostname/SERVER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.departmentNames", hasItem("IT")));
    }

    @Test
    void testPaginationAndFiltering() throws Exception {
        String uniqueHostname = "FILTER-SERVER-" + System.currentTimeMillis();
        ComputerSystemDto uniqueDto = ComputerSystemDto.builder()
                .hostname(uniqueHostname)
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(johnDoe.getId())
                .departmentIds(Set.of(itDept.getId()))
                .macAddress("00:1A:2B:3C:4D:" + String.format("%02X", System.nanoTime() % 256))
                .ipAddress("192.168.1." + (System.nanoTime() % 254 + 1))
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(uniqueDto))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/computer-systems")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/computer-systems/filter")
                .param("departmentId", itDept.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testValidationError() throws Exception {
        ComputerSystemDto invalidDto = ComputerSystemDto.builder()
                .hostname("")  // Invalid: empty
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(johnDoe.getId())
                .departmentIds(Set.of(itDept.getId()))
                .macAddress("invalid-mac")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalidDto))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNotFoundError() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateComputerSystem() throws Exception {
        String uniqueHostname = "UPDATE-SERVER-" + System.currentTimeMillis();
        ComputerSystemDto uniqueDto = ComputerSystemDto.builder()
                .hostname(uniqueHostname)
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(johnDoe.getId())
                .departmentIds(Set.of(itDept.getId()))
                .macAddress("00:1A:2B:3C:4D:" + String.format("%02X", System.nanoTime() % 256))
                .ipAddress("192.168.1." + (System.nanoTime() % 254 + 1))
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(uniqueDto))))
                .andExpect(status().isCreated());

        String responseBody = mockMvc.perform(get("/api/v1/computer-systems/hostname/" + uniqueHostname))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ComputerSystemDto createdDto = objectMapper.readValue(responseBody, ComputerSystemDto.class);

        createdDto.setUserId(janeDoe.getId());
        mockMvc.perform(put("/api/v1/computer-systems/" + createdDto.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(createdDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(janeDoe.getId().intValue())));
    }

    @Test
    void testDeleteComputerSystem() throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ComputerSystemDto createdDto = objectMapper.readValue(responseBody, ComputerSystemDto.class);

        mockMvc.perform(delete("/api/v1/computer-systems/" + createdDto.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/computer-systems/" + createdDto.getId()))
                .andExpect(status().isNotFound());
    }
}
