package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.shared.service.EmailNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComputerSystemController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "MY_APP_USER")
class ComputerSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComputerSystemService service;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ComputerSystemDto testDto;

    @BeforeEach
    void setUp() {
        testDto = ComputerSystemDto.builder()
                .id(1L)
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
    void testCreateComputerSystem() throws Exception {
        when(service.createComputerSystem(any(ComputerSystemDto.class))).thenReturn(testDto);

        mockMvc.perform(post("/api/v1/computer-systems")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.manufacturer", is("Dell")));

        verify(service, times(1)).createComputerSystem(any(ComputerSystemDto.class));
    }

    @Test
    void testGetComputerSystemById() throws Exception {
        when(service.getComputerSystemById(1L)).thenReturn(testDto);

        mockMvc.perform(get("/api/v1/computer-systems/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.hostname", is("SERVER-001")));

        verify(service, times(1)).getComputerSystemById(1L);
    }

    @Test
    void testGetComputerSystemByHostname() throws Exception {
        when(service.getComputerSystemByHostname("SERVER-001")).thenReturn(testDto);

        mockMvc.perform(get("/api/v1/computer-systems/hostname/SERVER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")));

        verify(service, times(1)).getComputerSystemByHostname("SERVER-001");
    }

    @Test
    void testGetAllComputerSystems() throws Exception {
        Page<ComputerSystemDto> page = new PageImpl<>(Arrays.asList(testDto), PageRequest.of(0, 20), 1);
        when(service.getAllComputerSystems(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/computer-systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(service, times(1)).getAllComputerSystems(any());
    }

    @Test
    void testFilterComputerSystems() throws Exception {
        Page<ComputerSystemDto> page = new PageImpl<>(Arrays.asList(testDto), PageRequest.of(0, 20), 1);
        when(service.filterComputerSystems(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/computer-systems/filter")
                .param("department", "IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(service, times(1)).filterComputerSystems(any(), any(), any(), any());
    }

    @Test
    void testUpdateComputerSystem() throws Exception {
        when(service.updateComputerSystem(eq(1L), any(ComputerSystemDto.class))).thenReturn(testDto);

        mockMvc.perform(put("/api/v1/computer-systems/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname", is("SERVER-001")));

        verify(service, times(1)).updateComputerSystem(eq(1L), any(ComputerSystemDto.class));
    }

    @Test
    void testDeleteComputerSystem() throws Exception {
        doNothing().when(service).deleteComputerSystem(1L);

        mockMvc.perform(delete("/api/v1/computer-systems/1"))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteComputerSystem(1L);
    }
}
