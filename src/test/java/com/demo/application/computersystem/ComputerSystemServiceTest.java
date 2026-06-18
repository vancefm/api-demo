package com.demo.application.computersystem;

import com.demo.application.department.DepartmentRepository;
import com.demo.domain.computersystem.ComputerSystem;
import com.demo.domain.computersystem.ComputerSystemDto;
import com.demo.domain.computersystem.ComputerSystemMapper;
import com.demo.domain.department.Department;
import com.demo.domain.user.User;
import com.demo.application.user.UserRepository;
import com.demo.shared.exception.DuplicateResourceException;
import com.demo.shared.exception.ResourceNotFoundException;
import com.demo.shared.security.CurrentUserService;
import com.demo.shared.security.FieldProjectionService;
import com.demo.shared.security.GrantService;
import com.demo.shared.security.ScopeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComputerSystemServiceTest {

    @Mock
    private ComputerSystemRepository repository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private GrantService grantService;
    @Mock
    private FieldProjectionService fieldProjectionService;

    private ComputerSystemMapper mapper;
    private ComputerSystemService service;

    private ComputerSystem testComputerSystem;
    private ComputerSystemDto testDto;
    private User testUser;
    private Department testDepartment;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> implClass = Class.forName(ComputerSystemMapper.class.getName() + "Impl");
        mapper = (ComputerSystemMapper) implClass.getDeclaredConstructor().newInstance();
        service = new ComputerSystemService(repository, userRepository, departmentRepository, mapper,
                currentUserService, grantService, fieldProjectionService);

        testDepartment = Department.builder().id(1L).name("IT").build();
        testUser = User.builder().id(1L).username("john.doe").email("john.doe@example.com").build();

        testComputerSystem = ComputerSystem.builder()
                .id(1L)
                .hostname("SERVER-001")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .systemUser(testUser)
                .departments(Set.of(testDepartment))
                .macAddress("00:1A:2B:3C:4D:5E")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();

        testDto = ComputerSystemDto.builder()
                .id(1L)
                .hostname("SERVER-001")
                .manufacturer("Dell")
                .model("PowerEdge R750")
                .userId(1L)
                .departmentIds(Set.of(1L))
                .macAddress("00:1A:2B:3C:4D:5E")
                .ipAddress("192.168.1.100")
                .networkName("PROD-NETWORK")
                .build();

        // Default authorization: full access; field projection is a pass-through.
        when(currentUserService.requireCurrentUser()).thenReturn(testUser);
        when(grantService.canAccess(any(), any(), eq("ComputerSystem"), any())).thenReturn(true);
        when(grantService.resolveScope(any(), eq("ComputerSystem"), any()))
                .thenReturn(new ScopeResult(true, false, Set.of()));
        when(departmentRepository.findAllById(any())).thenReturn(List.of(testDepartment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fieldProjectionService.filterReadable(any(), any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(2));
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
        verify(repository, times(1)).save(any(ComputerSystem.class));
    }

    @Test
    void testCreateComputerSystem_DuplicateHostname() {
        when(repository.findByHostname(testDto.getHostname())).thenReturn(Optional.of(testComputerSystem));

        assertThrows(DuplicateResourceException.class, () -> service.createComputerSystem(testDto));
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

        assertThrows(ResourceNotFoundException.class, () -> service.getComputerSystemById(99L));
    }

    @Test
    void testGetAllComputerSystems() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ComputerSystem> page = new PageImpl<>(List.of(testComputerSystem), pageable, 1);
        when(repository.findAll(ArgumentMatchersSpec(), eq(pageable))).thenReturn(page);

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
        when(repository.findById(1L)).thenReturn(Optional.of(testComputerSystem));

        service.deleteComputerSystem(1L);

        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteComputerSystem_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteComputerSystem(99L));
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

        assertThrows(ResourceNotFoundException.class, () -> service.getComputerSystemByHostname("NONEXISTENT"));
    }

    @SuppressWarnings("unchecked")
    private static Specification<ComputerSystem> ArgumentMatchersSpec() {
        return any(Specification.class);
    }
}
