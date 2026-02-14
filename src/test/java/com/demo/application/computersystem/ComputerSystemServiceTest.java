package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import com.demo.domain.computersystem.ComputerSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComputerSystemServiceTest {

    @Mock
    private ComputerSystemRepository repository;

    @InjectMocks
    private ComputerSystemService service;

    private ComputerSystem testComputerSystem;
    private ComputerSystemDto testDto;

    @BeforeEach
    void setUp() {
        testComputerSystem = ComputerSystem.builder()
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
    void testCreateComputerSystem_Success() {
        when(repository.findByHostname(testDto.getHostname())).thenReturn(Optional.empty());
        when(repository.findByMacAddress(testDto.getMacAddress())).thenReturn(Optional.empty());
        when(repository.findByIpAddress(testDto.getIpAddress())).thenReturn(Optional.empty());
        when(repository.save(any(ComputerSystem.class))).thenReturn(testComputerSystem);

        ComputerSystemDto result = service.createComputerSystem(testDto);

        assertNotNull(result);
        assertEquals(testDto.getHostname(), result.getHostname());
        assertEquals(testDto.getManufacturer(), result.getManufacturer());
        verify(repository, times(1)).save(any(ComputerSystem.class));
    }

    @Test
    void testCreateComputerSystem_DuplicateHostname() {
        when(repository.findByHostname(testDto.getHostname())).thenReturn(Optional.of(testComputerSystem));

        assertThrows(DuplicateResourceException.class, () -> {
            service.createComputerSystem(testDto);
        });

        verify(repository, never()).save(any(ComputerSystem.class));
    }

    @Test
    void testGetComputerSystemById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testComputerSystem));

        ComputerSystemDto result = service.getComputerSystemById(1L);

        assertNotNull(result);
        assertEquals(testComputerSystem.getId(), result.getId());
        assertEquals(testComputerSystem.getHostname(), result.getHostname());
    }

    @Test
    void testGetComputerSystemById_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.getComputerSystemById(99L);
        });
    }

    @Test
    void testGetAllComputerSystems() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ComputerSystem> page = new PageImpl<>(Arrays.asList(testComputerSystem), pageable, 1);

        when(repository.findAll(pageable)).thenReturn(page);

        Page<ComputerSystemDto> result = service.getAllComputerSystems(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testComputerSystem.getHostname(), result.getContent().get(0).getHostname());
    }

    @Test
    void testUpdateComputerSystem_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testComputerSystem));
        when(repository.save(any(ComputerSystem.class))).thenReturn(testComputerSystem);

        ComputerSystemDto result = service.updateComputerSystem(1L, testDto);

        assertNotNull(result);
        assertEquals(testDto.getHostname(), result.getHostname());
        verify(repository, times(1)).save(any(ComputerSystem.class));
    }

    @Test
    void testDeleteComputerSystem_Success() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        service.deleteComputerSystem(1L);

        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteComputerSystem_NotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteComputerSystem(99L);
        });

        verify(repository, never()).deleteById(any());
    }

    @Test
    void testGetComputerSystemByHostname_Success() {
        when(repository.findByHostname("SERVER-001")).thenReturn(Optional.of(testComputerSystem));

        ComputerSystemDto result = service.getComputerSystemByHostname("SERVER-001");

        assertNotNull(result);
        assertEquals(testComputerSystem.getHostname(), result.getHostname());
    }

    @Test
    void testGetComputerSystemByHostname_NotFound() {
        when(repository.findByHostname("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.getComputerSystemByHostname("NONEXISTENT");
        });
    }
}
