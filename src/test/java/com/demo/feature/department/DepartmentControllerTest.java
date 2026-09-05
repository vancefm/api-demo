package com.demo.feature.department;

import com.demo.integration.mail.EmailNotificationService;
import com.demo.platform.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService service;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private DepartmentDto testDto;

    @BeforeEach
    void setUp() {
        testDto = DepartmentDto.builder()
                .id(1L)
                .name("IT")
                .description("Information Technology")
                .build();
    }

    @Test
    void testCreateDepartment() throws Exception {
        when(service.createDepartment(any(DepartmentDto.class))).thenReturn(testDto);

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("IT")))
                .andExpect(jsonPath("$.description", is("Information Technology")));

        verify(service, times(1)).createDepartment(any(DepartmentDto.class));
    }

    @Test
    void testCreateDepartment_ValidationError() throws Exception {
        DepartmentDto invalid = DepartmentDto.builder().name("").build();

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalid))))
                .andExpect(status().isBadRequest());

        verify(service, never()).createDepartment(any(DepartmentDto.class));
    }

    @Test
    void testGetDepartmentById() throws Exception {
        when(service.getDepartmentById(1L)).thenReturn(testDto);

        mockMvc.perform(get("/api/v1/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("IT")));

        verify(service, times(1)).getDepartmentById(1L);
    }

    @Test
    void testGetDepartmentById_NotFound() throws Exception {
        when(service.getDepartmentById(99L)).thenThrow(new ResourceNotFoundException("Department with id 99 not found"));

        mockMvc.perform(get("/api/v1/departments/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllDepartments() throws Exception {
        Page<DepartmentDto> page = new PageImpl<>(Arrays.asList(testDto), PageRequest.of(0, 20), 1);
        when(service.getAllDepartments(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(service, times(1)).getAllDepartments(any());
    }

    @Test
    void testFilterDepartments() throws Exception {
        Page<DepartmentDto> page = new PageImpl<>(Arrays.asList(testDto), PageRequest.of(0, 20), 1);
        when(service.filterDepartments(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/departments/filter")
                .param("name", "IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("IT")));

        verify(service, times(1)).filterDepartments(eq("IT"), any(), any());
    }

    @Test
    void testUpdateDepartment() throws Exception {
        when(service.updateDepartment(eq(1L), any(DepartmentDto.class))).thenReturn(testDto);

        mockMvc.perform(put("/api/v1/departments/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(testDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("IT")));

        verify(service, times(1)).updateDepartment(eq(1L), any(DepartmentDto.class));
    }

    @Test
    void testDeleteDepartment() throws Exception {
        doNothing().when(service).deleteDepartment(1L);

        mockMvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteDepartment(1L);
    }
}
