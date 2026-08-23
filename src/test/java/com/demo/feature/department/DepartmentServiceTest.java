package com.demo.feature.department;

import com.demo.feature.computersystem.ComputerSystemRepository;
import com.demo.feature.user.UserRepository;
import com.demo.platform.exception.DuplicateResourceException;
import com.demo.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComputerSystemRepository computerSystemRepository;

    private DepartmentService service;

    private Department testDepartment;
    private DepartmentDto testDto;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(repository, userRepository, computerSystemRepository, new DepartmentMapper());

        testDepartment = Department.builder()
                .id(1L)
                .name("IT")
                .description("Information Technology")
                .build();

        testDto = DepartmentDto.builder()
                .id(1L)
                .name("IT")
                .description("Information Technology")
                .build();
    }

    @Test
    void testCreateDepartment_Success() {
        when(repository.existsByName("IT")).thenReturn(false);
        when(repository.save(any(Department.class))).thenReturn(testDepartment);

        DepartmentDto result = service.createDepartment(testDto);

        assertNotNull(result);
        assertEquals("IT", result.getName());
        verify(repository, times(1)).save(any(Department.class));
    }

    @Test
    void testCreateDepartment_DuplicateName() {
        when(repository.existsByName("IT")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.createDepartment(testDto));

        verify(repository, never()).save(any(Department.class));
    }

    @Test
    void testGetDepartmentById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testDepartment));

        DepartmentDto result = service.getDepartmentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("IT", result.getName());
    }

    @Test
    void testGetDepartmentById_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDepartmentById(99L));
    }

    @Test
    void testGetAllDepartments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Department> page = new PageImpl<>(Arrays.asList(testDepartment), pageable, 1);
        when(repository.findAll(pageable)).thenReturn(page);

        Page<DepartmentDto> result = service.getAllDepartments(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("IT", result.getContent().get(0).getName());
    }

    @Test
    void testUpdateDepartment_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(repository.save(any(Department.class))).thenReturn(testDepartment);

        DepartmentDto update = DepartmentDto.builder().name("IT").description("Updated description").build();
        DepartmentDto result = service.updateDepartment(1L, update);

        assertNotNull(result);
        verify(repository, times(1)).save(any(Department.class));
    }

    @Test
    void testUpdateDepartment_DuplicateName() {
        when(repository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(repository.existsByName("HR")).thenReturn(true);

        DepartmentDto update = DepartmentDto.builder().name("HR").build();

        assertThrows(DuplicateResourceException.class, () -> service.updateDepartment(1L, update));

        verify(repository, never()).save(any(Department.class));
    }

    @Test
    void testUpdateDepartment_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateDepartment(99L, testDto));
    }

    @Test
    void testDeleteDepartment_Success_RemovesAssociations() {
        when(repository.existsById(1L)).thenReturn(true);

        service.deleteDepartment(1L);

        verify(userRepository, times(1)).removeDepartmentAssociations(1L);
        verify(computerSystemRepository, times(1)).removeDepartmentAssociations(1L);
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteDepartment_NotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteDepartment(99L));

        verify(userRepository, never()).removeDepartmentAssociations(any());
        verify(computerSystemRepository, never()).removeDepartmentAssociations(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void testResolveDepartments_Success() {
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(testDepartment));

        Set<Department> result = service.resolveDepartments(List.of(1L));

        assertEquals(1, result.size());
        assertTrue(result.contains(testDepartment));
    }

    @Test
    void testResolveDepartments_NullOrEmptyReturnsEmptySet() {
        assertTrue(service.resolveDepartments(null).isEmpty());
        assertTrue(service.resolveDepartments(Collections.emptyList()).isEmpty());

        verify(repository, never()).findAllById(any());
    }

    @Test
    void testResolveDepartments_PartialMatchThrows() {
        when(repository.findAllById(List.of(1L, 99L))).thenReturn(List.of(testDepartment));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.resolveDepartments(List.of(1L, 99L)));

        assertTrue(ex.getMessage().contains("99"));
    }
}
