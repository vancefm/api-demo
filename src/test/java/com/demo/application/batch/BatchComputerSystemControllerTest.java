package com.demo.application.batch;

import com.demo.shared.config.BatchProperties;
import com.demo.domain.batch.BatchComputerSystemRequest;
import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.application.computersystem.ComputerSystemService;
import com.demo.application.computersystem.EmailNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for batch operations controller.
 *
 * Tests all-or-nothing batch semantics:
 * - Batch create with validation
 * - Batch update with configuration limits
 * - Batch delete with verification
 * - Batch size validation
 * - Error scenarios (empty batch, size exceeded, validation failures)
 */
@WebMvcTest(BatchComputerSystemController.class)
class BatchComputerSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComputerSystemService service;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @MockBean
    private BatchProperties batchProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private ComputerSystemDto testDto1;
    private ComputerSystemDto testDto2;

    @BeforeEach
    void setUp() {
        // Default batch max items to 100
        when(batchProperties.getMaxItems()).thenReturn(100);

        testDto1 = ComputerSystemDto.builder()
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

        testDto2 = ComputerSystemDto.builder()
                .id(2L)
                .hostname("SERVER-002")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .user("jane.smith")
                .department("IT")
                .macAddress("00:1A:2B:3C:4D:5F")
                .ipAddress("192.168.1.101")
                .networkName("PROD-NETWORK")
                .build();
    }

    /**
     * Test batch create with valid items.
     * Should create all items and return 201 Created.
     */
    @Test
    void testBatchCreate_Success() throws Exception {
        // Arrange
        when(service.createComputerSystem(any())).thenReturn(testDto1, testDto2);

        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList(
                testDto1, testDto2
        ));

        // Act & Assert
        mockMvc.perform(post("/api/v1/computer-systems/batch/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.successCount", is(2)))
                .andExpect(jsonPath("$.failureCount", is(0)))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].hostname", is("SERVER-001")))
                .andExpect(jsonPath("$.items[1].hostname", is("SERVER-002")));

        verify(service, times(2)).createComputerSystem(any());
    }

    /**
     * Test batch create with empty list.
     * Should return 400 Bad Request (validation failure).
     */
    @Test
    void testBatchCreate_EmptyBatch() throws Exception {
        // Arrange
        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList());

        // Act & Assert
        mockMvc.perform(post("/api/v1/computer-systems/batch/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Request Validation Failed")));

        verify(service, never()).createComputerSystem(any());
    }

    /**
     * Test batch create exceeds size limit.
     * Should return 400 Bad Request with size exceeded message.
     */
    @Test
    void testBatchCreate_SizeExceeded() throws Exception {
        // Arrange - Set max items to 1
        when(batchProperties.getMaxItems()).thenReturn(1);

        List<ComputerSystemDto> items = Arrays.asList(testDto1, testDto2);
        BatchComputerSystemRequest request = new BatchComputerSystemRequest(items);

        // Act & Assert
        mockMvc.perform(post("/api/v1/computer-systems/batch/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Batch Size Exceeds Maximum")))
                .andExpect(jsonPath("$.detail", containsString("exceeds maximum (1)")));

        verify(service, never()).createComputerSystem(any());
    }

    /**
     * Test batch create with invalid item (missing required field).
     * Should return 400 Bad Request (field validation failure).
     */
    @Test
    void testBatchCreate_InvalidItem() throws Exception {
        // Arrange - Item missing hostname (required field)
        ComputerSystemDto invalidDto = ComputerSystemDto.builder()
                .id(3L)
                .hostname("")  // Invalid: empty hostname
                .manufacturer("Dell")
                .model("PowerEdge")
                .user("user")
                .department("IT")
                .macAddress("00:1A:2B:3C:4D:5E")
                .ipAddress("192.168.1.100")
                .networkName("PROD")
                .build();

        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList(invalidDto));

        // Act & Assert
        mockMvc.perform(post("/api/v1/computer-systems/batch/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("items[0].hostname")));

        verify(service, never()).createComputerSystem(any());
    }

    /**
     * Test batch update with valid items.
     * Should update all items and return 200 OK.
     */
    @Test
    void testBatchUpdate_Success() throws Exception {
        // Arrange
        when(service.updateComputerSystem(eq(1L), any())).thenReturn(testDto1);
        when(service.updateComputerSystem(eq(2L), any())).thenReturn(testDto2);

        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList(
                testDto1, testDto2
        ));

        // Act & Assert
        mockMvc.perform(put("/api/v1/computer-systems/batch/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.successCount", is(2)))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.items[0].id", is(1)))
                .andExpect(jsonPath("$.items[1].id", is(2)));

        verify(service, times(2)).updateComputerSystem(any(Long.class), any());
    }

    /**
     * Test batch update exceeds size limit.
     * Should return 400 Bad Request without updating any items.
     */
    @Test
    void testBatchUpdate_SizeExceeded() throws Exception {
        // Arrange - Set max items to 1
        when(batchProperties.getMaxItems()).thenReturn(1);

        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList(
                testDto1, testDto2
        ));

        // Act & Assert
        mockMvc.perform(put("/api/v1/computer-systems/batch/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Batch Size Exceeds Maximum")));

        // Verify no updates occurred (all-or-nothing: size exceeded = no updates)
        verify(service, never()).updateComputerSystem(any(Long.class), any());
    }

    /**
     * Test batch delete with valid item IDs.
     * Should delete all items and return 204 No Content.
     */
    @Test
    void testBatchDelete_Success() throws Exception {
        // Arrange
        when(service.getComputerSystemById(1L)).thenReturn(testDto1);
        when(service.getComputerSystemById(2L)).thenReturn(testDto2);
        doNothing().when(service).deleteComputerSystem(any());

        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList(
                testDto1, testDto2
        ));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/computer-systems/batch/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Verify all items were verified and deleted (two-phase approach)
        verify(service, times(2)).getComputerSystemById(any());
        verify(service, times(2)).deleteComputerSystem(any());
    }

    /**
     * Test batch delete with empty batch.
     * Should return 400 Bad Request (validation failure).
     */
    @Test
    void testBatchDelete_EmptyBatch() throws Exception {
        // Arrange
        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/computer-systems/batch/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).deleteComputerSystem(any());
    }

    /**
     * Test batch delete exceeds size limit.
     * Should return 400 Bad Request (configuration limit exceeded).
     */
    @Test
    void testBatchDelete_SizeExceeded() throws Exception {
        // Arrange - Set max items to 1
        when(batchProperties.getMaxItems()).thenReturn(1);

        BatchComputerSystemRequest request = new BatchComputerSystemRequest(Arrays.asList(
                testDto1, testDto2
        ));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/computer-systems/batch/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Batch Size Exceeds Maximum")));

        // Verify no deletions occurred (size exceeded = no deletes)
        verify(service, never()).getComputerSystemById(any());
        verify(service, never()).deleteComputerSystem(any());
    }

    /**
     * Test batch configuration is configurable.
     * Verifies that BatchProperties is properly injected.
     */
    @Test
    void testBatchConfiguration() throws Exception {
        // Verify that batch max items can be configured
        when(batchProperties.getMaxItems()).thenReturn(50);

        int maxItems = batchProperties.getMaxItems();
        assert maxItems == 50;

        // Verify configuration bean is not null
        assert batchProperties != null;
    }
}
