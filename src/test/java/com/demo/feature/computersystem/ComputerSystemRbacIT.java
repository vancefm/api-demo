package com.demo.feature.computersystem;

import com.demo.feature.department.DepartmentDto;
import com.demo.feature.department.DepartmentRepository;
import com.demo.feature.security.rbac.Operation;
import com.demo.feature.security.rbac.PermissionDto;
import com.demo.feature.security.rbac.RoleAssignmentDto;
import com.demo.feature.security.rbac.RoleDto;
import com.demo.feature.security.rbac.RoleRepository;
import com.demo.feature.user.UserDto;
import com.demo.feature.user.UserRepository;
import com.demo.testsupport.AsAdminMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.demo.testsupport.AsAdminMockMvc.asUser;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC on computer systems, including the batch endpoint's all-or-nothing
 * guarantee when one item is out of the caller's scope.
 *
 * <p>Deliberately <em>not</em> {@code @Transactional}: the batch rollback only
 * exists as a real, observable effect when each request commits or rolls back
 * its own transaction. Everything created is removed in {@link #cleanUp()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AsAdminMockMvc.class)
class ComputerSystemRbacIT {

    private static final RequestPostProcessor USER1 = asUser("user1", "password1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComputerSystemRepository computerSystemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String stamp;
    private Long itId;
    private Long hrId;
    private Long ownerId;
    private Long itSystemId;
    private Long hrSystemId;

    private String json(Object value) {
        return Objects.requireNonNull(objectMapper.writeValueAsString(value));
    }

    private <T> T create(String url, Object body, Class<T> type) throws Exception {
        String response = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(body)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, type);
    }

    private ComputerSystemDto system(String suffix, Long departmentId) {
        int n = Math.abs(suffix.hashCode() % 200) + 10;
        return ComputerSystemDto.builder()
            .hostname("RBAC-" + stamp + "-" + suffix)
            .manufacturer("Dell")
            .model("PowerEdge R750")
            .userId(ownerId)
            .departmentIds(List.of(departmentId))
            .macAddress(String.format("02:%s:%02X:%02X:%02X:%02X", stamp.substring(stamp.length() - 2),
                n, (n * 7) % 256, (n * 13) % 256, (n * 17) % 256))
            .ipAddress("10." + (n % 250) + "." + ((n * 3) % 250) + "." + ((n * 5) % 250 + 1))
            .networkName("PROD-NETWORK")
            .build();
    }

    @BeforeEach
    void setUp() throws Exception {
        stamp = Long.toString(System.nanoTime() % 100_000_000L);
        itId = create("/api/v1/departments", DepartmentDto.builder().name("IT-" + stamp).build(), DepartmentDto.class).getId();
        hrId = create("/api/v1/departments", DepartmentDto.builder().name("HR-" + stamp).build(), DepartmentDto.class).getId();

        UserDto owner = new UserDto();
        owner.setUsername("owner." + stamp);
        owner.setEmail("owner." + stamp + "@example.com");
        ownerId = create("/api/v1/users", owner, UserDto.class).getId();

        Long user1Id = ensureUser1InDepartment(itId);

        itSystemId = create("/api/v1/computer-systems", system("it", itId), ComputerSystemDto.class).getId();
        hrSystemId = create("/api/v1/computer-systems", system("hr", hrId), ComputerSystemDto.class).getId();

        RoleDto operator = create("/api/v1/roles", RoleDto.builder()
            .name("System Operator " + stamp)
            .permissions(List.of(
                perm("hostname", Operation.READ),
                perm("manufacturer", Operation.READ),
                perm("model", Operation.READ),
                perm("departmentIds", Operation.READ),
                perm("*", Operation.CREATE)))
            .build(), RoleDto.class);

        create("/api/v1/users/" + user1Id + "/role-assignments",
            RoleAssignmentDto.builder().roleId(operator.getId()).departmentId(itId).build(),
            RoleAssignmentDto.class);
    }

    /**
     * {@code user1} may already exist from an earlier login in this shared
     * database; either way it must be a member of IT for this test.
     */
    private Long ensureUser1InDepartment(Long departmentId) throws Exception {
        return userRepository.findByUsername("user1").map(existing -> {
            UserDto dto = new UserDto();
            dto.setUsername("user1");
            dto.setEmail(existing.getEmail());
            dto.setDepartmentIds(List.of(departmentId));
            try {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/users/" + existing.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(dto)))
                    .andExpect(status().isOk());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return existing.getId();
        }).orElseGet(() -> {
            UserDto dto = new UserDto();
            dto.setUsername("user1");
            dto.setEmail("user1@example.com");
            dto.setDepartmentIds(List.of(departmentId));
            try {
                return create("/api/v1/users", dto, UserDto.class).getId();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static PermissionDto perm(String field, Operation op) {
        return PermissionDto.builder().entity("ComputerSystem").field(field).operation(op).build();
    }

    @AfterEach
    void cleanUp() {
        computerSystemRepository.findAll(ComputerSystemSpecifications.hostnameContains("RBAC-" + stamp))
            .forEach(computerSystemRepository::delete);
        userRepository.findByUsername("owner." + stamp).ifPresent(userRepository::delete);
        userRepository.findByUsername("user1").ifPresent(userRepository::delete);
        departmentRepository.findByName("IT-" + stamp).ifPresent(departmentRepository::delete);
        departmentRepository.findByName("HR-" + stamp).ifPresent(departmentRepository::delete);
        roleRepository.findByName("System Operator " + stamp).ifPresent(roleRepository::delete);
    }

    @Test
    void listIsScopedAndMasked() throws Exception {
        mockMvc.perform(get("/api/v1/computer-systems").with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(1)))
            .andExpect(jsonPath("$.content[0].id", is(itSystemId.intValue())))
            .andExpect(jsonPath("$.content[0].hostname", containsString("-it")))
            .andExpect(jsonPath("$.content[0].departmentIds[0]", is(itId.intValue())))
            .andExpect(jsonPath("$.content[0].ipAddress").doesNotExist())
            .andExpect(jsonPath("$.content[0].macAddress").doesNotExist())
            .andExpect(jsonPath("$.content[0].userId").doesNotExist());

        mockMvc.perform(get("/api/v1/computer-systems/" + hrSystemId).with(USER1))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/computer-systems/filter").param("departmentId", hrId.toString()).with(USER1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void batchCreateWithOneOutOfScopeItemCreatesNothing() throws Exception {
        ComputerSystemDto inScope = system("batch-ok", itId);
        ComputerSystemDto outOfScope = system("batch-hr", hrId);

        mockMvc.perform(post("/api/v1/computer-systems/batch/create").with(USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("items", List.of(inScope, outOfScope)))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail", containsString("CREATE ComputerSystem in department(s) [" + hrId + "]")));

        // All-or-nothing: the in-scope item was rolled back with the batch.
        mockMvc.perform(get("/api/v1/computer-systems/hostname/" + inScope.getHostname()))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/computer-systems/hostname/" + outOfScope.getHostname()))
            .andExpect(status().isNotFound());
    }

    @Test
    void batchCreateEntirelyInScopeSucceedsAndIsMasked() throws Exception {
        ComputerSystemDto a = system("batch-a", itId);
        ComputerSystemDto b = system("batch-b", itId);

        mockMvc.perform(post("/api/v1/computer-systems/batch/create").with(USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("items", List.of(a, b)))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.successCount", is(2)))
            .andExpect(jsonPath("$.items[0].hostname", containsString("batch-")))
            .andExpect(jsonPath("$.items[0].ipAddress").doesNotExist());

        mockMvc.perform(get("/api/v1/computer-systems/hostname/" + a.getHostname()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ipAddress", is(a.getIpAddress())));
    }
}
