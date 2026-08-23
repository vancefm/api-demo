package com.demo.feature.computersystem;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentRepository;
import com.demo.feature.user.UserRepository;
import com.demo.feature.user.User;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class ComputerSystemIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private ComputerSystemDto testDto;
    private User johnDoe;
    private User janeDoe;
    private Department itDepartment;

    @BeforeEach
    void setUp() {
        itDepartment = departmentRepository.findByName("IT")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name("IT")
                        .description("Information Technology")
                        .build()));

        johnDoe = userRepository.findByUsername("john.doe")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("john.doe")
                        .email("john.doe@example.com")
                        .build()));

        janeDoe = userRepository.findByUsername("jane.doe")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("jane.doe")
                        .email("jane.doe@example.com")
                        .build()));

        testDto = ComputerSystemDto.builder()
                .hostname("SERVER-001")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(johnDoe.getId())
                .departmentIds(List.of(itDepartment.getId()))
                .macAddress("00:1A:2B:3C:4D:5E")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();
    }

    @Test
    void testCreateAndRetrieveComputerSystem() throws Exception {
        // Create
        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.id", notNullValue()));

        // Retrieve by hostname
        mockMvc.perform(get("/api/v1/computer-systems/hostname/SERVER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.departments[0].name", is("IT")))
                .andExpect(jsonPath("$.departmentIds[0]", is(itDepartment.getId().intValue())));
    }

    @Test
    void testPaginationAndFiltering() throws Exception {
        // Create a computer system with unique values to avoid conflicts
        String uniqueHostname = "FILTER-SERVER-" + System.currentTimeMillis();
        ComputerSystemDto uniqueDto = ComputerSystemDto.builder()
                .hostname(uniqueHostname)
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(johnDoe.getId())
                .departmentIds(List.of(itDepartment.getId()))
                .macAddress("00:1A:2B:3C:4D:" + String.format("%02X", System.nanoTime() % 256))
                .ipAddress("192.168.1." + (System.nanoTime() % 254 + 1))
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(uniqueDto))))
                .andExpect(status().isCreated());

        // Get all with pagination
        mockMvc.perform(get("/api/v1/computer-systems")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        // Filter by department
        mockMvc.perform(get("/api/v1/computer-systems/filter")
                .param("departmentId", itDepartment.getId().toString()))
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
                .departmentIds(List.of(itDepartment.getId()))
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
        // Create with unique values
        String uniqueHostname = "UPDATE-SERVER-" + System.currentTimeMillis();
        ComputerSystemDto uniqueDto = ComputerSystemDto.builder()
                .hostname(uniqueHostname)
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(johnDoe.getId())
                .departmentIds(List.of(itDepartment.getId()))
                .macAddress("00:1A:2B:3C:4D:" + String.format("%02X", System.nanoTime() % 256))
                .ipAddress("192.168.1." + (System.nanoTime() % 254 + 1))
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(uniqueDto))))
                .andExpect(status().isCreated());

        // Get the created system
        String responseBody = mockMvc.perform(get("/api/v1/computer-systems/hostname/" + uniqueHostname))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ComputerSystemDto createdDto = objectMapper.readValue(responseBody, ComputerSystemDto.class);

        // Update
        createdDto.setUserId(janeDoe.getId());
        mockMvc.perform(put("/api/v1/computer-systems/" + createdDto.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(createdDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(janeDoe.getId().intValue())));
    }

    @Test
    void testDeleteComputerSystem() throws Exception {
        // Create
        String responseBody = mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ComputerSystemDto createdDto = objectMapper.readValue(responseBody, ComputerSystemDto.class);

        // Delete
        mockMvc.perform(delete("/api/v1/computer-systems/" + createdDto.getId()))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/v1/computer-systems/" + createdDto.getId()))
                .andExpect(status().isNotFound());
    }
}
