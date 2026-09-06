package com.demo.feature.computersystem;

import com.demo.feature.department.Department;
import com.demo.feature.department.DepartmentMapper;
import com.demo.feature.department.DepartmentService;
import com.demo.feature.security.rbac.AccessControl;
import com.demo.feature.security.rbac.Operation;
import com.demo.feature.user.User;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComputerSystemServiceTest {

    @Mock
    private ComputerSystemRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private AccessControl accessControl;

    private ComputerSystemMapper mapper;

    private ComputerSystemService service;

    private ComputerSystem testComputerSystem;
    private ComputerSystemDto testDto;
    private User testUser;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        mapper = new ComputerSystemMapper(new DepartmentMapper());
        service = new ComputerSystemService(repository, userRepository, departmentService, mapper, accessControl);
        // Permissive access control: require* are no-ops, readableDepartments()
        // is Optional.empty() (global) and masking is an identity.
        lenient().when(accessControl.filterReadable(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));

        testUser = User.builder()
                .id(1L)
                .username("john.doe")
                .email("john.doe@example.com")
                .build();

        testDepartment = Department.builder()
                .id(1L)
                .name("IT")
                .build();

        testComputerSystem = ComputerSystem.builder()
                .id(1L)
                .hostname("SERVER-001")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .systemUser(testUser)
                .macAddress("00:1A:2B:3C:4D:5E")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();

        testComputerSystem.getDepartmentLinks().add(ComputerSystemDepartment.builder()
                .computerSystem(testComputerSystem)
                .department(testDepartment)
                .build());

        testDto = ComputerSystemDto.builder()
                .id(1L)
                .hostname("SERVER-001")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(1L)
                .departmentIds(List.of(1L))
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(departmentService.resolveDepartments(testDto.getDepartmentIds())).thenReturn(Set.of(testDepartment));
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

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ComputerSystemDto> result = service.getAllComputerSystems(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testComputerSystem.getHostname(), result.getContent().get(0).getHostname());
    }

    @Test
    void testUpdateComputerSystem_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testComputerSystem));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(departmentService.resolveDepartments(testDto.getDepartmentIds())).thenReturn(Set.of(testDepartment));
        when(repository.save(any(ComputerSystem.class))).thenReturn(testComputerSystem);

        ComputerSystemDto result = service.updateComputerSystem(1L, testDto);

        assertNotNull(result);
        assertEquals(testDto.getHostname(), result.getHostname());
        verify(repository, times(1)).save(any(ComputerSystem.class));
    }

    @Test
    void testDeleteComputerSystem_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testComputerSystem));

        service.deleteComputerSystem(1L);

        verify(repository, times(1)).delete(testComputerSystem);
    }

    @Test
    void testDeleteComputerSystem_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteComputerSystem(99L);
        });

        verify(repository, never()).delete(any(ComputerSystem.class));
    }

    @Test
    void testDeleteComputerSystem_DeniedDeletesNothing() {
        when(repository.findById(1L)).thenReturn(Optional.of(testComputerSystem));
        doThrow(new AccessDeniedException("Not permitted to DELETE ComputerSystem in department(s) [1]"))
            .when(accessControl).requireAccess(eq("ComputerSystem"), eq(Operation.DELETE), any());

        assertThrows(AccessDeniedException.class, () -> service.deleteComputerSystem(1L));

        verify(repository, never()).delete(any(ComputerSystem.class));
    }

    @Test
    void testCreateComputerSystem_DeniedBeforeDuplicateChecks() {
        doThrow(new AccessDeniedException("Not permitted to CREATE ComputerSystem in department(s) [1]"))
            .when(accessControl).requireAccess(eq("ComputerSystem"), eq(Operation.CREATE), any());

        assertThrows(AccessDeniedException.class, () -> service.createComputerSystem(testDto));

        verify(repository, never()).findByHostname(any());
        verify(repository, never()).save(any(ComputerSystem.class));
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
