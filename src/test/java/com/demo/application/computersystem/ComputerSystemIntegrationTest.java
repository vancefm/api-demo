package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystemDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComputerSystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ComputerSystemDto testDto;

    @BeforeEach
    void setUp() {
        testDto = ComputerSystemDto.builder()
                .hostname("SERVER-001")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .user("john.doe")
                .department("IT")
                .macAddress("00:1A:2B:3C:4D:5E")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();
    }

    @Test
    void testCreateAndRetrieveComputerSystem() throws Exception {
        // Create
        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.id", notNullValue()));

        // Retrieve by hostname
        mockMvc.perform(get("/api/v1/computer-systems/hostname/SERVER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.department", is("IT")));
    }

    @Test
    void testPaginationAndFiltering() throws Exception {
        // Create a computer system with unique values to avoid conflicts
        String uniqueHostname = "FILTER-SERVER-" + System.currentTimeMillis();
        ComputerSystemDto uniqueDto = ComputerSystemDto.builder()
                .hostname(uniqueHostname)
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .user("john.doe")
                .department("IT")
                .macAddress("00:1A:2B:3C:4D:" + String.format("%02X", System.nanoTime() % 256))
                .ipAddress("192.168.1." + (System.nanoTime() % 254 + 1))
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uniqueDto)))
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
                .param("department", "IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testValidationError() throws Exception {
        ComputerSystemDto invalidDto = ComputerSystemDto.builder()
                .hostname("")  // Invalid: empty
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .user("john.doe")
                .department("IT")
                .macAddress("invalid-mac")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
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
                .user("john.doe")
                .department("IT")
                .macAddress("00:1A:2B:3C:4D:" + String.format("%02X", System.nanoTime() % 256))
                .ipAddress("192.168.1." + (System.nanoTime() % 254 + 1))
                .networkName("PROD-NETWORK")
                .build();

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uniqueDto)))
                .andExpect(status().isCreated());

        // Get the created system
        String responseBody = mockMvc.perform(get("/api/v1/computer-systems/hostname/" + uniqueHostname))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ComputerSystemDto createdDto = objectMapper.readValue(responseBody, ComputerSystemDto.class);

        // Update
        createdDto.setUser("jane.doe");
        mockMvc.perform(put("/api/v1/computer-systems/" + createdDto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user", is("jane.doe")));
    }

    @Test
    void testDeleteComputerSystem() throws Exception {
        // Create
        String responseBody = mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDto)))
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
